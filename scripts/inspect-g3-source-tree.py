#!/usr/bin/env python3
"""Offline, fail-closed source and patch preflight for a LineageOS 24 mustang tree.

The inspector never fetches repositories. A VERIFIED result means the local tree
has the expected repo manifest revision, required paths, bridge signature anchors,
and a clean SystemUI build-file patch preflight. It is not a build result.
"""
import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

REQUIRED_DIRS = [
    "device/google/mustang", "device/google/muzel", "device/google/laguna",
    "device/google/muzel-kernels", "vendor/google/mustang",
    "frameworks/base/packages/SystemUI",
]
REQUIRED_FILES = [
    "frameworks/base/packages/SystemUI/Android.bp",
    "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/CentralSurfacesImpl.java",
    "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/notification/collection/NotifPipeline.kt",
    "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/notification/collection/notifcollection/NotifCollectionListener.java",
    "frameworks/base/packages/SystemUI/src/com/android/systemui/media/controls/domain/pipeline/MediaDataManager.kt",
    "frameworks/base/packages/SystemUI/src/com/android/systemui/privacy/PrivacyItemController.kt",
    "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/policy/KeyguardStateController.java",
]
OVERLAYABLE_CANDIDATES = [
    "frameworks/base/core/res/res/values/overlayable.xml",
    "frameworks/base/packages/SystemUI/res/values/overlayable.xml",
]
SIGNATURES = {
    REQUIRED_FILES[2]: ["fun addCollectionListener(", "fun removeCollectionListener("],
    REQUIRED_FILES[3]: ["fun onEntryAdded(", "fun onEntryUpdated(", "fun onEntryRemoved("],
    REQUIRED_FILES[4]: ["fun addListener(", "fun removeListener(", "fun onMediaDataLoaded(", "fun onMediaDataRemoved("],
    REQUIRED_FILES[5]: ["fun addCallback(", "fun removeCallback(", "fun onPrivacyItemsChanged("],
    REQUIRED_FILES[6]: ["void addCallback(", "void removeCallback(", "onKeyguardShowingChanged()"],
}
CALL_CANDIDATES = [
    "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/",
    "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/",
]


def git_output(root: Path, *args: str) -> str:
    try:
        result = subprocess.run(["git", "-C", str(root), *args], capture_output=True,
                                text=True, check=False, timeout=10)
        return result.stdout.strip() if result.returncode == 0 else ""
    except (OSError, subprocess.TimeoutExpired):
        return ""


def inspect_tree(aosp_root: Path, patch_file: Path | None = None) -> dict:
    root = aosp_root.resolve()
    missing_dirs = [d for d in REQUIRED_DIRS if not (root / d).is_dir()]
    missing_files = [f for f in REQUIRED_FILES if not (root / f).is_file()]
    found_overlayables = [p for p in OVERLAYABLE_CANDIDATES if (root / p).is_file()]

    framework_root = root / "frameworks/base"
    branch = git_output(framework_root, "rev-parse", "--abbrev-ref", "HEAD") if framework_root.is_dir() else ""
    revision = git_output(framework_root, "rev-parse", "HEAD") if framework_root.is_dir() else ""
    manifest_text = ""
    manifest_path = root / ".repo/manifest.xml"
    if manifest_path.is_file():
        manifest_text = manifest_path.read_text(encoding="utf-8", errors="replace")
    branch_ok = branch == "lineage-24.0"
    if manifest_text:
        projects = [
            node for node in re.findall(r"<project\b([^>]*)>", manifest_text)
            if re.search(r"\bpath=[\"']frameworks/base[\"']", node)
        ]
        revision_match = bool(projects) and any(
            re.search(r"\brevision=[\"']lineage-24\.0[\"']", node) for node in projects
        )
        branch_ok = branch_ok or revision_match

    signature_missing = []
    for rel, anchors in SIGNATURES.items():
        path = root / rel
        if not path.is_file():
            continue
        source = path.read_text(encoding="utf-8", errors="replace")
        signature_missing.extend(f"{rel}: {anchor}" for anchor in anchors if anchor not in source)

    call_source_files = []
    for rel in CALL_CANDIDATES:
        folder = root / rel
        if folder.is_dir():
            try:
                for path in folder.rglob("*"):
                    if path.is_file() and "call" in path.name.lower():
                        call_source_files.append(str(path.relative_to(root)))
                        if len(call_source_files) >= 100:
                            break
            except OSError:
                # Inaccessible subtrees cannot establish a missing API; report only readable hits.
                pass
        if len(call_source_files) >= 100:
            break
    call_source_files = sorted(set(call_source_files))

    target = root / "frameworks/base/packages/SystemUI/Android.bp"
    patch_result = "NOT_CHECKED"
    patch_error = ""
    git_available = shutil.which("git") is not None
    if patch_file is not None and target.is_file() and git_available and framework_root.is_dir():
        try:
            result = subprocess.run(["git", "-C", str(framework_root), "apply", "--check", str(patch_file.resolve())],
                                    capture_output=True, text=True, check=False, timeout=20)
            patch_result = "PASS" if result.returncode == 0 else "FAIL"
            patch_error = (result.stderr or result.stdout).strip()
        except (OSError, subprocess.TimeoutExpired) as exc:
            patch_result, patch_error = "FAIL", str(exc)
    elif patch_file is not None and target.is_file():
        patch_result, patch_error = "FAIL", "git executable or frameworks/base checkout unavailable"

    expected_revision = "lineage-24.0"
    verified = (not missing_dirs and not missing_files and bool(found_overlayables)
                and branch_ok and not signature_missing and patch_result == "PASS")
    return {
        "aosp_root": str(root),
        "status": "VERIFIED" if verified else "UNVERIFIED_FAIL_CLOSED",
        "verification_scope": "LOCAL_SOURCE_AND_PATCH_PREFLIGHT_ONLY",
        "expected_revision": expected_revision,
        "frameworks_base_branch": branch or None,
        "frameworks_base_commit": revision or None,
        "revision_verified": branch_ok,
        "required_dirs_missing": missing_dirs,
        "required_files_missing": missing_files,
        "overlayable_files_found": found_overlayables,
        "signature_anchors_missing": signature_missing,
        "call_state_source_candidates": call_source_files,
        "call_state_api_verified": False,
        "call_state_api_provenance_required": "Record repository URL, exact commit, source file path, declarations and lifecycle owner after inspecting target source; no call adapter is authorized by this report.",
        "systemui_patch_preflight": patch_result,
        "systemui_patch_error": patch_error or None,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Offline fail-closed G3 LineageOS 24 mustang source inspector")
    parser.add_argument("--aosp-root", required=True, help="Path to a synced LineageOS 24 source root")
    parser.add_argument("--patch-file", help="Patch to preflight against frameworks/base (defaults to repository patch)")
    args = parser.parse_args()
    patch_file = Path(args.patch_file) if args.patch_file else Path(__file__).resolve().parents[1] / "systemui/smartisland/patches/0001-SystemUI-Android.bp-add-OriginPixelSmartIslandCore.patch"
    report = inspect_tree(Path(args.aosp_root), patch_file)
    print(json.dumps(report, indent=2))
    return 0 if report["status"] == "VERIFIED" else 2


if __name__ == "__main__":
    sys.exit(main())

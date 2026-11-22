#!/usr/bin/env python3
"""
Offline-safe patch dry-run test for 0001-SystemUI-Android.bp-add-OriginPixelSmartIslandCore.patch.

Verifies unified diff format, target path, and clean `git apply --check` against a
minimal fixture matching verified LineageOS/android_frameworks_base@lineage-24.0
packages/SystemUI/Android.bp (lines 488-496).
"""
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / "systemui/smartisland/patches/0001-SystemUI-Android.bp-add-OriginPixelSmartIslandCore.patch"
SOONG_BP = ROOT / "systemui/smartisland/Android.bp"

FIXTURE_LINES = [
    "android_library {\n",
    '    name: "SystemUI-core",\n',
    "    static_libs: [\n",
    '        "//frameworks/libs/systemui:compilelib",\n',
    '        "com.android.systemui.bundle.phone_api",\n',
    '        "SystemUI-res",\n',
    '        "WifiTrackerLib",\n',
    '        "WindowManager-Shell",\n',
    "    ],\n",
    "}\n",
]


def test_soong_module_matches_patch():
    bp = SOONG_BP.read_text(encoding="utf-8")
    assert 'name: "OriginPixelSmartIslandCore"' in bp
    assert '"//frameworks/base/packages/SystemUI:__subpackages__"' in bp
    assert 'sdk_version: "core_current"' in bp


def test_patch_applies_cleanly_to_fixture():
    with tempfile.TemporaryDirectory(prefix="op-patch-dryrun-") as tmp:
        root = Path(tmp)
        target = root / "packages/SystemUI/Android.bp"
        target.parent.mkdir(parents=True, exist_ok=True)
        # Pad to match upstream line numbers (488)
        padding = ["// pad\n"] * 485
        target.write_text("".join(padding + FIXTURE_LINES), encoding="utf-8")

        subprocess.run(["git", "init", "-q"], cwd=root, check=True)
        subprocess.run(["git", "add", "packages/SystemUI/Android.bp"], cwd=root, check=True)
        subprocess.run(
            ["git", "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-qm", "fixture"],
            cwd=root,
            check=True,
        )
        subprocess.run(["git", "apply", "--check", str(PATCH)], cwd=root, check=True)
        subprocess.run(["git", "apply", str(PATCH)], cwd=root, check=True)
        patched = target.read_text(encoding="utf-8")
        assert '"OriginPixelSmartIslandCore",' in patched


if __name__ == "__main__":
    test_soong_module_matches_patch()
    test_patch_applies_cleanly_to_fixture()
    print("ALL Patch Dry-Run & Soong Alignment Tests PASSED (2/2)")

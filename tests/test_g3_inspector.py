#!/usr/bin/env python3
"""Deterministic fail-closed checks for G3 source and patch inspection."""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/inspect-g3-source-tree.py"


def run_inspector(root: Path):
    proc = subprocess.run(
        [sys.executable, str(SCRIPT), "--aosp-root", str(root)],
        capture_output=True, text=True, check=False,
    )
    return proc.returncode, json.loads(proc.stdout)


def test_fail_closed_on_empty_dir():
    with tempfile.TemporaryDirectory(prefix="op-g3-empty-") as tmp:
        code, report = run_inspector(Path(tmp))
        assert code == 2
        assert report["status"] == "UNVERIFIED_FAIL_CLOSED"
        assert len(report["required_dirs_missing"]) == 6
        assert len(report["required_files_missing"]) == 7
        assert report["revision_verified"] is False
        assert report["systemui_patch_preflight"] == "NOT_CHECKED"
        assert report["call_state_api_verified"] is False


def test_mock_layout_and_signature_tokens_do_not_verify_checkout():
    """Even complete paths and signature-like text cannot fake git revision/provenance."""
    with tempfile.TemporaryDirectory(prefix="op-g3-mock-") as tmp:
        root = Path(tmp)
        for rel in [
            "device/google/mustang", "device/google/muzel", "device/google/laguna",
            "device/google/muzel-kernels", "vendor/google/mustang",
            "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone",
            "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/notification/collection/notifcollection",
            "frameworks/base/packages/SystemUI/src/com/android/systemui/media/controls/domain/pipeline",
            "frameworks/base/packages/SystemUI/src/com/android/systemui/privacy",
            "frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/policy",
            "frameworks/base/packages/SystemUI/res/values",
        ]:
            (root / rel).mkdir(parents=True, exist_ok=True)
        base = root / "frameworks/base/packages/SystemUI"
        (base / "Android.bp").write_text('android_library { name: "SystemUI-core" }\n', encoding="utf-8")
        (base / "src/com/android/systemui/statusbar/phone/CentralSurfacesImpl.java").write_text("public void start() {}", encoding="utf-8")
        fixtures = {
            "src/com/android/systemui/statusbar/notification/collection/NotifPipeline.kt": "fun addCollectionListener( fun removeCollectionListener(",
            "src/com/android/systemui/statusbar/notification/collection/notifcollection/NotifCollectionListener.java": "fun onEntryAdded( fun onEntryUpdated( fun onEntryRemoved(",
            "src/com/android/systemui/media/controls/domain/pipeline/MediaDataManager.kt": "fun addListener( fun removeListener( fun onMediaDataLoaded( fun onMediaDataRemoved(",
            "src/com/android/systemui/privacy/PrivacyItemController.kt": "fun addCallback( fun removeCallback( fun onPrivacyItemsChanged(",
            "src/com/android/systemui/statusbar/policy/KeyguardStateController.java": "void addCallback( void removeCallback( onKeyguardShowingChanged()",
        }
        for rel, content in fixtures.items():
            (base / rel).write_text(content, encoding="utf-8")
        (base / "res/values/overlayable.xml").write_text("<resources/>", encoding="utf-8")
        code, report = run_inspector(root)
        assert code == 2
        assert report["status"] == "UNVERIFIED_FAIL_CLOSED"
        assert report["required_dirs_missing"] == []
        assert report["required_files_missing"] == []
        assert report["signature_anchors_missing"] == []
        assert report["revision_verified"] is False
        assert report["call_state_api_verified"] is False


if __name__ == "__main__":
    test_fail_closed_on_empty_dir()
    test_mock_layout_and_signature_tokens_do_not_verify_checkout()
    print("ALL G3 Source Inspector Tests PASSED (2/2)")

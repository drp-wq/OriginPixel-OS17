#!/usr/bin/env python3
"""Unit tests for fail-closed Gate G3 source tree inspector."""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

SCRIPT = Path("/srv/originpixel-os17/scripts/inspect-g3-source-tree.py")


def run_inspector(root: Path):
    proc = subprocess.run(
        [sys.executable, str(SCRIPT), "--aosp-root", str(root)],
        capture_output=True,
        text=True,
        check=False,
    )
    return proc.returncode, json.loads(proc.stdout)


def test_fail_closed_on_empty_dir():
    with tempfile.TemporaryDirectory(prefix="op-g3-empty-") as tmp:
        code, report = run_inspector(Path(tmp))
        assert code == 2, f"Expected fail-closed exit 2, got {code}"
        assert report["status"] == "UNVERIFIED_FAIL_CLOSED"
        assert len(report["required_dirs_missing"]) == 6


def test_verified_on_complete_mock_tree():
    with tempfile.TemporaryDirectory(prefix="op-g3-mock-") as tmp:
        root = Path(tmp)
        for d in [
            "device/google/mustang",
            "device/google/muzel",
            "device/google/laguna",
            "device/google/muzel-kernels",
            "vendor/google/mustang",
            "frameworks/base/packages/SystemUI/res/values",
        ]:
            (root / d).mkdir(parents=True, exist_ok=True)
        (root / "frameworks/base/packages/SystemUI/res/values/overlayable.xml").write_text(
            "<resources><overlayable name='SystemUI'/></resources>", encoding="utf-8"
        )
        code, report = run_inspector(root)
        assert code == 0, f"Expected exit 0 on complete mock tree, got {code}"
        assert report["status"] == "VERIFIED"
        assert report["required_dirs_missing"] == []


if __name__ == "__main__":
    test_fail_closed_on_empty_dir()
    test_verified_on_complete_mock_tree()
    print("ALL G3 Source Inspector Tests PASSED (2/2)")

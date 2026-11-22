#!/usr/bin/env python3
"""Run source-only checks against this isolated sanitized repository."""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def test_tracked_tree_has_no_release_or_credential_material():
    subprocess.run(
        [sys.executable, str(ROOT / "scripts/check-source-tree-security.py")],
        cwd=ROOT,
        check=True,
    )


if __name__ == "__main__":
    test_tracked_tree_has_no_release_or_credential_material()
    print("ALL Source Tree Security Tests PASSED (1/1)")

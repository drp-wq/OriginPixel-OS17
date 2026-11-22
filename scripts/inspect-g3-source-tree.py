#!/usr/bin/env python3
"""
OriginPixel OS 17 — Fail-Closed Gate G3 Source Tree Inspector.

Verifies that a synced LineageOS 24.0 (mustang) source tree contains all required
device/vendor/kernel paths and locates authoritative overlayable.xml files before
any L1 RRO or L3 SystemUI integration is permitted.

Exit codes:
  0 — All required paths and overlayable targets verified.
  2 — Fail-closed: one or more required paths/targets missing or unverified.
"""
import argparse
import json
import sys
from pathlib import Path

REQUIRED_DIRS = [
    "device/google/mustang",
    "device/google/muzel",
    "device/google/laguna",
    "device/google/muzel-kernels",
    "vendor/google/mustang",
    "frameworks/base/packages/SystemUI",
]

OVERLAYABLE_CANDIDATES = [
    "frameworks/base/core/res/res/values/overlayable.xml",
    "frameworks/base/packages/SystemUI/res/values/overlayable.xml",
]


def inspect_tree(aosp_root: Path) -> dict:
    missing_dirs = [d for d in REQUIRED_DIRS if not (aosp_root / d).is_dir()]
    found_overlayables = [
        p for p in OVERLAYABLE_CANDIDATES if (aosp_root / p).is_file()
    ]
    missing_overlayables = [
        p for p in OVERLAYABLE_CANDIDATES if not (aosp_root / p).is_file()
    ]

    passed = (len(missing_dirs) == 0) and (len(found_overlayables) > 0)
    return {
        "aosp_root": str(aosp_root),
        "status": "VERIFIED" if passed else "UNVERIFIED_FAIL_CLOSED",
        "required_dirs_missing": missing_dirs,
        "overlayable_files_found": found_overlayables,
        "overlayable_files_missing": missing_overlayables,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Fail-closed G3 LineageOS 24.0 source inspector")
    parser.add_argument("--aosp-root", required=True, help="Path to synced LineageOS 24.0 root")
    args = parser.parse_args()

    result = inspect_tree(Path(args.aosp_root))
    print(json.dumps(result, indent=2))
    return 0 if result["status"] == "VERIFIED" else 2


if __name__ == "__main__":
    sys.exit(main())

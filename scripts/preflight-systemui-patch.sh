#!/usr/bin/env bash
# OriginPixel OS 17 — Automated SystemUI Source-Tree Preflight & Patch Dry-Run (Fail-Closed)
# Usage: ./scripts/preflight-systemui-patch.sh <AOSP_ROOT>
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <AOSP_ROOT>" >&2
  exit 2
fi

AOSP_ROOT="$1"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PATCH_FILE="${REPO_ROOT}/systemui/smartisland/patches/0001-SystemUI-Android.bp-add-OriginPixelSmartIslandCore.patch"

echo "=== 1. Fail-Closed G3 Source Tree Inspection ==="
python3 "${REPO_ROOT}/scripts/inspect-g3-source-tree.py" --aosp-root "$AOSP_ROOT"

echo "=== 2. Verify CentralSurfacesImpl.java Anchor ==="
CS_FILE="${AOSP_ROOT}/frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/CentralSurfacesImpl.java"
grep -q "public void start()" "$CS_FILE"

echo "=== 3. Dry-Run Patch Check (packages/SystemUI/Android.bp) ==="
git -C "${AOSP_ROOT}/frameworks/base" apply --check "$PATCH_FILE"

echo "PASS: SystemUI source-tree preflight and patch dry-run succeeded."

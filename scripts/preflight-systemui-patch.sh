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

echo "=== Offline source, revision, bridge-signature, and patch preflight ==="
python3 "${REPO_ROOT}/scripts/inspect-g3-source-tree.py" --aosp-root "$AOSP_ROOT" --patch-file "$PATCH_FILE"
echo "PASS: local source and patch preflight only (NOT A SYSTEMUI OR ROM BUILD)."

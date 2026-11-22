#!/usr/bin/env bash
# OriginPixel OS 17 — Reproducible External High-Memory Build-Host & Clean Baseline Script (G2/G3)
# Target: Google Pixel 10 Pro XL (mustang) — LineageOS 24.0 (lineage-24.0, Android 17 / SDK 37)
#
# SAFETY: Fail-closed hardware guard refuses to run on under-provisioned hosts (e.g., 8 GiB VPS).
set -euo pipefail

MIN_RAM_GB=32
MIN_DISK_GB=500
AOSP_ROOT="${1:-$HOME/lineage-24.0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

RAM_KIB=$(awk '/MemTotal/ {print $2}' /proc/meminfo)
RAM_GB=$((RAM_KIB / 1024 / 1024))
BUILD_PARENT=$(dirname "$AOSP_ROOT")
while [ ! -d "$BUILD_PARENT" ] && [ "$BUILD_PARENT" != "/" ]; do
  BUILD_PARENT=$(dirname "$BUILD_PARENT")
done
DISK_KIB=$(df -Pk "$BUILD_PARENT" | awk 'NR == 2 {print $4}')
DISK_GB=$((DISK_KIB / 1024 / 1024))

echo "=== PREFLIGHT ONLY — LineageOS 24.0 mustang build host ==="
echo "Host: $(hostname) | RAM: ${RAM_GB} GiB | Free Disk on build path filesystem: ${DISK_GB} GiB | CPUs: $(nproc)"

if [ "$RAM_GB" -lt "$MIN_RAM_GB" ] || [ "$DISK_GB" -lt "$MIN_DISK_GB" ]; then
  echo "BLOCKED: Host does not meet minimum Android 17 / LineageOS 24.0 build requirements."
  echo "Required: >= ${MIN_RAM_GB} GiB RAM and >= ${MIN_DISK_GB} GiB free disk on the build path filesystem."
  echo "Refusing to sync AOSP or start build on this host."
  exit 2
fi

mkdir -p "$AOSP_ROOT"
cd "$AOSP_ROOT"

if [ ! -d ".repo" ]; then
  repo init -u https://github.com/LineageOS/android.git -b lineage-24.0 --git-lfs
fi

mkdir -p .repo/local_manifests
cp "${REPO_ROOT}/manifests/originpixel_mustang.xml" .repo/local_manifests/originpixel_mustang.xml

repo sync -c --no-clone-bundle --optimized-fetch --prune -j"$(nproc)"

echo "=== Running Fail-Closed G3 Source Tree Inspector ==="
python3 "${REPO_ROOT}/scripts/inspect-g3-source-tree.py" --aosp-root "$AOSP_ROOT"

echo "=== Building Unmodified LineageOS 24.0 Baseline (lineage_mustang) ==="
export USE_CCACHE=1
export CCACHE_EXEC=/usr/bin/ccache
# shellcheck disable=SC1091
source build/envsetup.sh
breakfast mustang
brunch mustang

echo "=== Exporting Pinned Manifest & SHA-256 Checksums ==="
OUT_ARTIFACT_DIR="${AOSP_ROOT}/originpixel-g3-artifacts"
mkdir -p "$OUT_ARTIFACT_DIR"
repo manifest -r -o "${OUT_ARTIFACT_DIR}/lineage-24.0-mustang-pinned.xml"
sha256sum out/target/product/mustang/lineage-24.0-*.zip > "${OUT_ARTIFACT_DIR}/SHA256SUMS"
if [ ! -s "${OUT_ARTIFACT_DIR}/SHA256SUMS" ]; then
  echo "FAIL: no baseline ROM zip matched the expected output pattern; no build success claim." >&2
  exit 1
fi
echo "G3 CLEAN BASELINE COMPLETE: ${OUT_ARTIFACT_DIR}"

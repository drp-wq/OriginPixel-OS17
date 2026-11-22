#!/usr/bin/env bash
# OriginPixel OS 17 — Lightweight Kotlin Stub Compile-Readiness Check
# Compiles OriginPixelSmartIslandCore (javac) + upstream-matched stubs + SmartIslandSystemUiBridge.kt (kotlinc).
# Fails closed with exit 2 if kotlinc is not installed on the current host.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! command -v kotlinc >/dev/null 2>&1; then
  echo "SKIP/BLOCKED: kotlinc is not installed on $(hostname). Run on a host with kotlinc (e.g., GitHub Actions or G3 build host)."
  exit 2
fi

TMP_DIR=$(mktemp -d /tmp/op-kt-check.XXXXXX)
trap 'rm -rf "$TMP_DIR"' EXIT

javac -J-Xmx512m -d "$TMP_DIR/java" \
  systemui/smartisland/com/originpixel/systemui/smartisland/*.java

kotlinc -J-Xmx512m -cp "$TMP_DIR/java" -d "$TMP_DIR/kotlin.jar" \
  tests/stubs/*.kt \
  systemui/smartisland/patches/SmartIslandSystemUiBridge.kt

echo "PASS: SmartIslandSystemUiBridge.kt compiled cleanly against lineage-24.0 stubs."

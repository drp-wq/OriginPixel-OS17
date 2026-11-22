#!/usr/bin/env bash
# OriginPixel OS 17 — Lightweight Kotlin Stub Compile-Readiness Check
# Compiles OriginPixelSmartIslandCore (javac) + upstream-matched stubs + SmartIslandSystemUiBridge.kt (kotlinc).
# In CI, installs a pinned compiler artifact and verifies its published SHA-256.
# On other hosts, fails closed with exit 2 if kotlinc is unavailable.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

KOTLIN_VERSION="2.4.21"
KOTLIN_COMPILER_SHA256="7cc140e76daf416a0424a5557f99afd7ca89ebd463ea1101261e3e01c33e75cd"
KOTLINC_BIN="$(command -v kotlinc || true)"

if [[ -z "$KOTLINC_BIN" && "${GITHUB_ACTIONS:-}" == "true" ]]; then
  for tool in curl unzip; do
    command -v "$tool" >/dev/null 2>&1 || { echo "FAIL: $tool unavailable for pinned Kotlin compiler setup" >&2; exit 2; }
  done
  KOTLIN_HOME="${RUNNER_TEMP:-/tmp}/kotlin-compiler-${KOTLIN_VERSION}"
  mkdir -p "$KOTLIN_HOME"
  curl --fail --location --silent --show-error --proto '=https' --tlsv1.2 \
    "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" \
    --output "${KOTLIN_HOME}/compiler.zip"
  printf '%s  %s\n' "$KOTLIN_COMPILER_SHA256" "${KOTLIN_HOME}/compiler.zip" | sha256sum --check --status || {
    echo "FAIL: pinned Kotlin compiler SHA-256 mismatch" >&2
    exit 2
  }
  unzip -q "${KOTLIN_HOME}/compiler.zip" -d "$KOTLIN_HOME"
  KOTLINC_BIN="${KOTLIN_HOME}/kotlinc/bin/kotlinc"
elif [[ -z "$KOTLINC_BIN" ]]; then
  echo "SKIP/BLOCKED: kotlinc is not installed on $(hostname)." >&2
  exit 2
fi

TMP_DIR=$(mktemp -d /tmp/op-kt-check.XXXXXX)
trap 'rm -rf "$TMP_DIR"' EXIT

javac -J-Xmx512m -d "$TMP_DIR/java" \
  systemui/smartisland/com/originpixel/systemui/smartisland/*.java

"$KOTLINC_BIN" -J-Xmx512m -cp "$TMP_DIR/java" -d "$TMP_DIR/kotlin.jar" \
  tests/stubs/*.kt \
  systemui/smartisland/patches/SmartIslandSystemUiBridge.kt

echo "PASS: SmartIslandSystemUiBridge.kt compiled cleanly against lineage-24.0 stubs."

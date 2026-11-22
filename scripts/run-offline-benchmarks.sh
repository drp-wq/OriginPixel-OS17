#!/usr/bin/env bash
# Host microbenchmarks only. No Android/device performance claim is made.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${TMPDIR:-/tmp}/originpixel-offline-benchmarks"
mkdir -p "$OUT_DIR/classes"
javac -d "$OUT_DIR/classes" \
  "$ROOT/motion/src/com/originpixel/motion/SpringIntegrator.java" \
  "$ROOT/performance/OfflineBenchmark.java"
java -cp "$OUT_DIR/classes" com.originpixel.performance.OfflineBenchmark
echo "HOST_MICROBENCH_ONLY: target device frame budget is 8.33ms at 120Hz; this result is not comparable to Android frame timing."

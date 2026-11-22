#!/usr/bin/env python3
"""Bounded offline checks for the host benchmark and release preflight guard."""
import hashlib
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def test_benchmark_runs_within_bound_and_marks_host_only():
    env = os.environ.copy()
    with tempfile.TemporaryDirectory(prefix="op-benchmark-test-") as tmp:
        env["TMPDIR"] = tmp
        result = subprocess.run(
            ["bash", str(ROOT / "scripts/run-offline-benchmarks.sh")],
            cwd=ROOT, env=env, capture_output=True, text=True, timeout=90,
        )
    assert result.returncode == 0, result.stderr
    assert "HOST_MICROBENCH_ONLY" in result.stdout
    assert "iterations=" in result.stdout and "ns_per_step=" in result.stdout


def test_release_preflight_pass_and_reject_cases():
    script = ROOT / "scripts/release-preflight.sh"
    with tempfile.TemporaryDirectory(prefix="op-release-test-") as tmp:
        unsigned = Path(tmp) / "candidate.zip"
        signed = Path(tmp) / "candidate-signed.zip"
        payload = b"offline release fixture only\n"
        unsigned.write_bytes(payload)
        signed.write_bytes(payload)
        digest = hashlib.sha256(payload).hexdigest()

        accepted = subprocess.run(["bash", str(script), str(signed), digest], capture_output=True, text=True)
        assert accepted.returncode == 0, accepted.stderr
        assert "RELEASE PREFLIGHT ONLY" in accepted.stdout

        for artifact, checksum in (
            (str(unsigned), digest),
            (str(signed), "0" * 64),
            (str(signed), "bad-checksum"),
        ):
            rejected = subprocess.run(["bash", str(script), artifact, checksum], capture_output=True, text=True)
            assert rejected.returncode != 0, f"unexpected acceptance: {artifact} {checksum}"


if __name__ == "__main__":
    test_benchmark_runs_within_bound_and_marks_host_only()
    test_release_preflight_pass_and_reject_cases()
    print("ALL Offline Preflight Tests PASSED (2/2)")

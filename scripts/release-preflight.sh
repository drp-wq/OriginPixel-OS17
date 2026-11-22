#!/usr/bin/env bash
# Fail-closed metadata and secret-location checks before a release candidate is assembled.
set -euo pipefail
if [[ $# -ne 2 ]]; then echo "Usage: $0 <rom.zip> <expected-sha256>" >&2; exit 64; fi
artifact="$1"
expected="$2"
if [[ ! -f "$artifact" || -L "$artifact" ]]; then echo "FAIL: artifact must be a regular file" >&2; exit 2; fi
if [[ ! "$expected" =~ ^[a-fA-F0-9]{64}$ ]]; then echo "FAIL: expected SHA-256 must be 64 hex characters" >&2; exit 2; fi
actual="$(sha256sum "$artifact" | cut -d' ' -f1)"
if [[ "${actual,,}" != "${expected,,}" ]]; then echo "FAIL: SHA-256 mismatch" >&2; exit 2; fi
case "$artifact" in *-signed.zip) echo "PASS: signed artifact checksum matches";; *) echo "FAIL: artifact name must identify signed output (*-signed.zip)" >&2; exit 2;; esac
echo "RELEASE PREFLIGHT ONLY: does not verify key custody, OTA metadata/signature, rollback index, or device installation."

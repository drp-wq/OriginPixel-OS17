#!/usr/bin/env python3
"""Fail-closed checks that the sanitized CI checkout contains source-only files."""
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
FORBIDDEN_SUFFIXES = {
    ".apk", ".img", ".zip", ".tar", ".tar.gz", ".tar.zst", ".jks", ".keystore",
    ".pk8", ".p12", ".pfx", ".pem", ".key",
}
SECRET_NAME = re.compile(r"(^|[._-])(secret|credential|private[-_]?key|signing[-_]?key)([._-]|$)", re.I)
SECRET_CONTENT = re.compile(
    r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|"
    r"AKIA[0-9A-Z]{16}|"
    r"gh[pousr]_[A-Za-z0-9_]{30,}"
)


def tracked_files() -> list[Path]:
    result = subprocess.run(
        ["git", "-C", str(ROOT), "ls-files", "-z"],
        check=True, capture_output=True,
    )
    return [ROOT / entry.decode("utf-8") for entry in result.stdout.split(b"\0") if entry]


def checkout_files() -> list[Path]:
    """Include staged/unstaged untracked additions while respecting ignore rules."""
    result = subprocess.run(
        ["git", "-C", str(ROOT), "ls-files", "--cached", "--others", "--exclude-standard", "-z"],
        check=True, capture_output=True,
    )
    return [ROOT / entry.decode("utf-8") for entry in result.stdout.split(b"\0") if entry]


def main() -> int:
    problems = []
    files = checkout_files()
    for path in files:
        rel = path.relative_to(ROOT)
        lowered = rel.as_posix().lower()
        if any(lowered.endswith(suffix) for suffix in FORBIDDEN_SUFFIXES):
            problems.append(f"release/archive/credential artifact tracked: {rel}")
        if any(SECRET_NAME.search(part) for part in rel.parts):
            problems.append(f"sensitive filename/path tracked: {rel}")
        if path.is_symlink():
            problems.append(f"symlink tracked in source-only tree: {rel}")
            continue
        try:
            if path.stat().st_size <= 2_000_000:
                content = path.read_bytes()
                if SECRET_CONTENT.search(content.decode("utf-8", errors="ignore")):
                    problems.append(f"private key or credential-like token in tracked file: {rel}")
        except OSError as exc:
            problems.append(f"cannot inspect tracked file {rel}: {exc}")

    if problems:
        print("FAIL: source-only tree security check", file=sys.stderr)
        for problem in problems:
            print(f" - {problem}", file=sys.stderr)
        return 1
    print(f"PASS: inspected {len(files)} tracked and non-ignored checkout files; no release artifacts, secret paths, symlinks, or credential markers found")
    return 0


if __name__ == "__main__":
    sys.exit(main())

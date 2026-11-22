# Sprint 05 CI Validation Report

## Scope

Changes are confined to the sanitized `ci/source-only-validation` checkout. The pipeline keeps its read-only GitHub token permissions, SHA-pinned Actions, no persisted checkout credentials, and existing Java and schema tests.

The workflow now validates shell syntax, checks checkout content for release artifacts and credential markers, compiles `SmartIslandSystemUiBridge.kt` against the checked-in stubs, and runs bounded offline benchmark and release-preflight cases. The Kotlin CI fallback pins JetBrains Kotlin 2.4.21's compiler distribution ZIP and verifies the release-published SHA-256 before extraction and execution. Existing local `kotlinc` is used when available; unavailable compilers fail closed outside GitHub Actions.

## Local validation

Passed in this environment:

- XML manifest parsing and the existing design-token, G3 inspector, CI smoke, upstream wiring, and patch dry-run checks.
- `bash -n scripts/*.sh` and Python bytecode compilation for the source checks.
- Sanitized tree check: 64 tracked and non-ignored checkout files inspected with no release artifacts, secret paths, symlinks, or credential markers.
- Kotlin bridge stub compilation using local `kotlinc-jvm 2.4.21`.
- Offline benchmark execution and release preflight accepted/rejected fixtures.
- Existing Java tests: SmartIsland contract (11 cases), simulated adapter (6), SystemUI boundary (12), launcher (7 assertions), surface state (6), and spring integrator (3).

The separate `tests/test_dashboard_security_and_a11y.py` is not part of this sanitized source-only checkout's CI: it hard-codes `/srv/originpixel-os17/dashboard/...` and a live `10.77.0.1` endpoint, neither of which exists or is reachable here. It was inspected but not run against another checkout or network service.

## Hosted Actions

The existing remote branch head was confirmed at `877f59e885a42d5f5f5e1ff75fad2270506254e3`. Existing push and manual-dispatch runs for that prior commit both completed successfully (run IDs `45069664531` and `45069664880`). This runner mounts `.git` read-only, so the local commit could not be created (`.git/index.lock`: read-only file system). The connected GitHub write tools were also rejected by the environment approval gate. No remote write was made, so no new hosted run was triggered; the prior successful runs do not validate these uncommitted changes. Once these prepared changes are committed and pushed from an environment that permits Git writes, inspect that Actions run before treating hosted validation as complete.

## Explicit limits

This validates bridge syntax against lightweight local stubs only. It does not prove actual SystemUI integration, compile the full Android tree, build a ROM, verify signing key custody or release provenance, benchmark a Pixel device, or flash hardware. No ROM build or device operation was performed.

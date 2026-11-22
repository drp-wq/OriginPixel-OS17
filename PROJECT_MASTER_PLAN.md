# OriginPixel OS 17 — Project Master Plan

**Product:** OriginPixel OS 17 (Original Android 17 Custom ROM with OriginOS 7-inspired aesthetics & native SmartIsland)  
**Target Device:** Google Pixel 10 Pro XL (`mustang`, Tensor G5 / `laguna`)  
**Verified Baseline:** LineageOS 24.0 (`lineage-24.0`)  
**Controller Root:** `/srv/originpixel-os17` (on `srv2115613`)

## Execution Gates & Status

| Gate | Name | Status | Blocking Dependency |
| :--- | :--- | :--- | :--- |
| **Phase 0** | Infrastructure & Safety Audit | **COMPLETE** (local restore verified; off-site DR pending) | Independent off-site encrypted backup verification |
| **G0** | Compatibility Research | **PASSED** | Verified LineageOS 24.0 (`mustang`, `muzel`, `laguna`, `muzel-kernels`, `TheMuppets`) |
| **G1** | Safety & Recovery | **COMPLETE (LOCAL)** | Off-site disaster recovery and user/device readiness remain external |
| **G2** | Build Environment | **BLOCKED (Host Provisioning)** | Provision 32–64 GiB RAM, 500+ GB NVMe build host |
| **G3** | Clean Baseline | **BLOCKED (G2/source checkout absent)** | Compile unmodified LineageOS 24.0 `mustang` |
| **G4** | Hardware Validation | **GATED (User Auth)** | Explicit user authorization required before touching physical phone |
| **G5** | Original Design System (L1) | **LOCAL SPEC COMPLETE; RRO TARGETS BLOCKED ON G3** | Android overlayable provenance, resource compilation, screenshots |
| **G6** | Launcher (L2) | **FRAMEWORK-NEUTRAL CONTRACT COMPLETE; APP INTEGRATION BLOCKED ON G3** | Android launcher module, widgets, recents, instrumentation |
| **G7** | Native SystemUI & SmartIsland (L3) | **INTEGRATION-READY CONTRACTS; TARGET WIRING BLOCKED ON G3** | Verified upstream signatures, Soong build, device validation |
| **G8** | Motion & Framework (L4) | **HOST-TESTABLE SPEC COMPLETE; DEVICE TUNING BLOCKED ON G3/G4** | 120Hz frame pacing and haptic device checks |
| **G9** | Optimization & Benchmarking | **OFFLINE HARNESS COMPLETE; DEVICE BENCHMARKS BLOCKED ON G3/G4** | Perfetto, thermal, battery, physical device |
| **G10** | Release Preparation | **GUARDED DOCUMENTATION/SCRIPTS COMPLETE; RELEASE BLOCKED ON G3/G4** | Build artifacts, isolated signing, OTA verification |

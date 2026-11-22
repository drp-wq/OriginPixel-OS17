# Gate G3 — External High-Memory Build-Host Runbook (LineageOS 24.0 `mustang`)

**Status:** READY FOR EXECUTION (Zero resources purchased or provisioned)
**Target:** Google Pixel 10 Pro XL (`mustang`, Android 17 / SDK 37, `lineage-24.0`)

---

## 1. Host Resource & Cost Estimates

| Option | Specs | Est. Cost | Clean Build Time |
| :--- | :--- | :--- | :---: |
| **On-Demand Cloud VM** (GCP `c3-standard-44` / AWS `c7i.8xlarge` + 750 GB NVMe) | 32–44 vCPU / 64–128 GiB RAM / 750 GB NVMe | ~$1.50–$2.50/hr (~$5–$12/build if stopped when idle) | ~1–2 hours |
| **Dedicated Bare-Metal** (Hetzner AX52 / AX102) | 16–32 threads / 64–128 GiB RAM / 1–2 TB NVMe | ~$65–$120/month flat | ~1–2 hours |

---

## 2. Step-by-Step Build & SystemUI Integration Runbook

1. **Bootstrap Host:** Run `scripts/prepare-build-host.sh` (installs LineageOS 24.0 deps, `repo`, `ccache`).
2. **Clean Baseline (Gate G3):** Run `scripts/build-mustang-baseline.sh` (syncs `lineage-24.0` + `manifests/originpixel_mustang.xml`, runs `scripts/inspect-g3-source-tree.py`, compiles unmodified `brunch mustang`, exports pinned manifest & SHA-256).
3. **Wire `OriginPixelSmartIslandCore` & `SmartIslandSystemUiBridge.kt`:**
   - Copy `systemui/smartisland/` → `<AOSP_ROOT>/vendor/originpixel/smartisland/`
   - Copy `systemui/smartisland/patches/SmartIslandSystemUiBridge.kt` → `<AOSP_ROOT>/frameworks/base/packages/SystemUI/src/com/android/systemui/originpixel/smartisland/SmartIslandSystemUiBridge.kt`
   - In `frameworks/base/packages/SystemUI/Android.bp`, add `"OriginPixelSmartIslandCore"` to `SystemUI-core` `static_libs`.
   - Inject `SmartIslandSystemUiBridge` (`@SysUISingleton`) into `CentralSurfacesImpl.java` and call `start()` / `onGoingToSleep()` / `onUserSwitched()`.
4. **Targeted SystemUI Compile & Verification:**
   - `m SystemUI` (verifies full Soong + Dagger + Kotlin compilation against real Android 17 `frameworks/base`).
   - Keep `ORIGINPIXEL_ENABLE_RRO := false` until `overlayable.xml` targets are verified in the synced tree.
5. **Full ROM Build:** `brunch mustang` → verify SHA-256 and archive build logs back to `srv2115613`.

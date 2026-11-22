# OriginPixel OS 17 — LineageOS 24.0 SystemUI Patch Application & Readiness Report

**Date:** 2026-11-22
**Branch:** `feat/systemui-upstream-wiring`
**VPS Resource Audit:** `7.8 GiB RAM` (`5.3 GiB` avail), `32 GiB` free on `/` — **full AOSP sync/build strictly prohibited on this VPS**.

---

## 1. Selective Upstream Verification Summary (`LineageOS/android_frameworks_base@lineage-24.0`)

| Target | Verified Upstream Path | Status |
| :--- | :--- | :--- |
| Notification Pipeline | `packages/SystemUI/src/com/android/systemui/statusbar/notification/collection/NotifPipeline.kt` | **VERIFIED** (migrated to Kotlin in Android 17) |
| Media Session | `packages/SystemUI/src/com/android/systemui/media/controls/domain/pipeline/MediaDataManager.kt` | **VERIFIED** |
| Privacy Indicators | `packages/SystemUI/src/com/android/systemui/privacy/PrivacyItemController.kt` (`Callback.onPrivacyItemsChanged(List<PrivacyItem>)`) | **VERIFIED** |
| Keyguard State | `packages/SystemUI/src/com/android/systemui/statusbar/policy/KeyguardStateController.java` (`Callback.onKeyguardShowingChanged()`) | **VERIFIED** |
| Status Bar Host | `packages/SystemUI/src/com/android/systemui/statusbar/phone/CentralSurfacesImpl.java` | **VERIFIED** |
| SystemUI Build Target | `packages/SystemUI/Android.bp` (`SystemUI-core`) | **VERIFIED** |
| `overlayable.xml` (core/res & SystemUI/res) | Directory listing returned `[]` for `*overlay*` files | **UNVERIFIED / FAIL-CLOSED** (`ORIGINPIXEL_ENABLE_RRO := false`) |

---

## 2. Patch Application Instructions (Execute ONLY on External High-Memory G3 Build Host)

1. Complete Gate G3 clean baseline build first (`scripts/build-mustang-baseline.sh`).
2. Copy `systemui/smartisland/` into `<AOSP_ROOT>/vendor/originpixel/smartisland/` (contains `Android.bp` defining `OriginPixelSmartIslandCore`).
3. Copy `systemui/smartisland/patches/SmartIslandSystemUiBridge.kt` into `<AOSP_ROOT>/frameworks/base/packages/SystemUI/src/com/android/systemui/originpixel/smartisland/SmartIslandSystemUiBridge.kt`.
4. Add `"OriginPixelSmartIslandCore"` to `static_libs` of `SystemUI-core` in `frameworks/base/packages/SystemUI/Android.bp`.
5. Wire `SmartIslandSystemUiBridge.start()` / `onGoingToSleep()` / `onUserSwitched()` inside `CentralSurfacesImpl.java`.
6. Compile targeted SystemUI build: `m SystemUI` and run Soong/Robolectric tests before building full `brunch mustang`.

---

## 3. Exact Remaining Blocker
- **External High-Memory Build Host (Gate G2/G3):** `SmartIslandSystemUiBridge.kt` cannot be compiled with `kotlinc`/Soong on the development VPS without the synced `lineage-24.0` `frameworks/base` tree (`>= 32–64 GiB RAM`, `>= 500 GB NVMe`). No paid compute has been provisioned.

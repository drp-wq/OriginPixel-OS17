# G0 Compatibility Report — OriginPixel OS 17

**Status:** PASSED (Authoritative Online Sources Verified; Local Build Target Pending G3)  
**Last Verified:** 2026-11-22T16:33:15Z  
**Target Device:** Google Pixel 10 Pro XL  
**Verified Codename:** `mustang`  
**Verified SoC:** Google Tensor G5 (platform codename `laguna`, 3nm TSMC)  
**Target OS:** Android 17 (API / SDK Level 37)  
**Selected Baseline:** **LineageOS 24.0 (`lineage-24.0`)**

---

## 1. Authoritative Verification Evidence (2026-11-22)

Every claim below was independently verified via direct HTTP/API queries on `2026-11-22`:

| Source | Endpoint | HTTP | Verification Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| LineageOS Wiki | `https://wiki.lineageos.org/devices/mustang/` | `200` | Confirms `mustang`, `Android 17`, maintainer `mikeioannina` | **VERIFIED** |
| LineageOS Downloads API | `https://download.lineageos.org/api/v2/devices/mustang/builds` | `200` | Official signed nightly: `lineage-24.0-20261116-nightly-mustang-signed.zip` (`os_patch_level: 2026-11-01`, `os_sdk_level: 37`) | **VERIFIED** |
| Core Manifest | `LineageOS/android` (`lineage-24.0`) | `200` | Head SHA `913c6baa35ff` | **VERIFIED** |
| Device Tree | `LineageOS/android_device_google_mustang` (`lineage-24.0`) | `200` | Head SHA `38a7ebc31613` | **VERIFIED** |
| Pixel 10 Family Common | `LineageOS/android_device_google_muzel` (`lineage-24.0`) | `200` | Head SHA `60f3d1ade406` | **VERIFIED** |
| Tensor G5 SoC Common | `LineageOS/android_device_google_laguna` (`lineage-24.0`) | `200` | Head SHA `7ef360132041` | **VERIFIED** |
| Prebuilt Kernel & Modules | `LineageOS/android_device_google_muzel-kernels` (`lineage-24.0`) | `200` | Head SHA `5a905ac0afd7` | **VERIFIED** |
| Proprietary Vendor Blobs | `TheMuppets/proprietary_vendor_google_mustang` (`lineage-24.0`) | `200` | Head SHA `ae55e6f16bcd` | **VERIFIED** |
| Local Compilation (`brunch mustang`) | Dedicated build host (Gate G2/G3) | N/A | Not yet executed; `srv2115613` lacks RAM/disk | **UNVERIFIED (Pending G2/G3)** |
| SystemUI Internal Class Signatures | Synced `frameworks/base/packages/SystemUI` | N/A | Source tree not yet synced | **UNVERIFIED (Pending G2/G3)** |

---

## 2. Baseline Decision & Tradeoffs

- **Selected Baseline:** LineageOS 24.0 (`lineage-24.0`).
- **Rationale:** Official `mustang` nightly builds exist on `download.lineageos.org` (SDK 37), and all 6 required GitHub repositories have live `lineage-24.0` branches.
- **Fallback (Not Needed):** Pure AOSP Android 17 + manual Pixel 10 Pro XL bring-up.

## 3. Safety & Compatibility Constraints

- **Physical Device:** Production phone running important apps. **NO unlocking, wiping, flashing, or rebooting** is permitted without separate explicit user authorization (Gate G4).
- **Banking / Trading / Authenticator Apps:** See [G1_SAFETY_AND_RECOVERY.md](file:///srv/originpixel-os17/docs/G1_SAFETY_AND_RECOVERY.md).

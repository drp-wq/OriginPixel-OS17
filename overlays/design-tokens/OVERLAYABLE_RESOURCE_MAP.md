# OriginPixel OS 17 — L1 Overlayable Resource Map

**Status:** DRAFT — All Target RRO Bindings `UNVERIFIED_PENDING_G3_TREE`

Per project policy: *Use runtime resource overlays (RROs) only for resources that are actually overlayable.*

## Verification Audit (2026-11-22)
- Direct check of `https://raw.githubusercontent.com/LineageOS/android_frameworks_base/lineage-24.0/core/res/res/values/overlayable.xml` returned **HTTP 404**.
- Full `lineage-24.0` source tree is not yet synced on `srv2115613` (awaiting G2 build host).
- Consequently, **no RRO APK is built or claimed compatible** yet. All prospective bindings in [overlayable-map.json](file:///srv/originpixel-os17/overlays/design-tokens/overlayable-map.json) are explicitly marked `UNVERIFIED_PENDING_G3_TREE` and enforced by [test_design_tokens.py](file:///srv/originpixel-os17/tests/test_design_tokens.py).

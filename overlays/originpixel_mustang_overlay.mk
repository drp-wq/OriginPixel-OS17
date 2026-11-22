# OriginPixel OS 17 — Product Integration Fragment for lineage_mustang
# Verified upstream product: device/google/muzel/lineage_mustang.mk (Android 17 / CP3A.261005.005)
#
# Include from vendor/originpixel/config/originpixel_mustang.mk after G3 clean baseline passes.

# Native SmartIsland core library (Soong module: systemui/smartisland/Android.bp)
ORIGINPIXEL_SMARTISLAND_ENABLED := true

# L1 Runtime Resource Overlays — STRICTLY DISABLED until overlayable.xml is verified in G3 tree
ORIGINPIXEL_ENABLE_RRO := false

ifeq ($(ORIGINPIXEL_SMARTISLAND_ENABLED),true)
PRODUCT_PACKAGES += \
    OriginPixelSmartIslandCore
endif

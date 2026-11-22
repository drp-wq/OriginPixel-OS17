#!/usr/bin/env python3
"""Static verification tests for LineageOS 24.0 SystemUI wiring patch & fail-closed gates."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BRIDGE_KT = ROOT / "systemui/smartisland/patches/SmartIslandSystemUiBridge.kt"
OVERLAY_MK = ROOT / "overlays/originpixel_mustang_overlay.mk"
OVERLAY_MAP = ROOT / "overlays/design-tokens/overlayable-map.json"


def test_bridge_uses_verified_apis_only():
    src = BRIDGE_KT.read_text(encoding="utf-8")
    required_imports = [
        "com.android.systemui.statusbar.notification.collection.NotifPipeline",
        "com.android.systemui.media.controls.domain.pipeline.MediaDataManager",
        "com.android.systemui.privacy.PrivacyItemController",
        "com.android.systemui.statusbar.policy.KeyguardStateController",
        "com.originpixel.systemui.smartisland.SmartIslandContract",
        "com.originpixel.systemui.smartisland.SmartIslandUiController",
    ]
    for imp in required_imports:
        assert imp in src, f"Missing verified upstream/core import: {imp}"

    forbidden = ["vivo", "originos", "Class.forName", "setAccessible(true)"]
    for bad in forbidden:
        assert bad.lower() not in src.lower(), f"Forbidden pattern found: {bad}"


def test_fail_closed_rro_gates_preserved():
    mk = OVERLAY_MK.read_text(encoding="utf-8")
    assert "ORIGINPIXEL_ENABLE_RRO := false" in mk, "RROs must remain fail-closed disabled until G3"

    omap = json.loads(OVERLAY_MAP.read_text(encoding="utf-8"))
    for entry in omap["mappings"]:
        assert entry["rro_compatibility_status"] == "UNVERIFIED_PENDING_G3_TREE", (
            f"Overlayable target prematurely marked verified: {entry['originpixel_token']}"
        )


if __name__ == "__main__":
    test_bridge_uses_verified_apis_only()
    test_fail_closed_rro_gates_preserved()
    print("ALL Upstream Wiring Patch & Fail-Closed Gate Tests PASSED (2/2)")

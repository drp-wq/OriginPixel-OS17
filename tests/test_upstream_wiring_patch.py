#!/usr/bin/env python3
"""Static verification tests for hardened LineageOS 24.0 SystemUI wiring bridge & fail-closed gates."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BRIDGE_KT = ROOT / "systemui/smartisland/patches/SmartIslandSystemUiBridge.kt"
OVERLAY_MK = ROOT / "overlays/originpixel_mustang_overlay.mk"
OVERLAY_MAP = ROOT / "overlays/design-tokens/overlayable-map.json"


def test_bridge_uses_verified_apis_and_balanced_lifecycle():
    src = BRIDGE_KT.read_text(encoding="utf-8")

    # Verified upstream & DI imports
    for token in [
        "com.android.systemui.dagger.SysUISingleton",
        "javax.inject.Inject",
        "@SysUISingleton",
        "@Inject constructor",
        "com.android.systemui.statusbar.notification.collection.NotifPipeline",
        "com.android.systemui.media.controls.domain.pipeline.MediaDataManager",
        "com.android.systemui.privacy.PrivacyItemController",
        "com.android.systemui.statusbar.policy.KeyguardStateController",
    ]:
        assert token in src, f"Missing verified API/DI token: {token}"

    # Balanced listener registration & removal (no callback leaks)
    pairs = [
        ("notifPipeline.addCollectionListener(", "notifPipeline.removeCollectionListener("),
        ("mediaDataManager.addListener(", "mediaDataManager.removeListener("),
        ("privacyItemController.addCallback(", "privacyItemController.removeCallback("),
        ("keyguardStateController.addCallback(", "keyguardStateController.removeCallback("),
    ]
    for add_call, remove_call in pairs:
        assert add_call in src, f"Missing registration: {add_call}"
        assert remove_call in src, f"Missing cleanup/removal: {remove_call}"

    # Notification updates & strict redaction (PRIVATE + SECRET)
    assert "override fun onEntryAdded(" in src
    assert "override fun onEntryUpdated(" in src
    assert "override fun onEntryRemoved(" in src
    assert "Notification.VISIBILITY_PRIVATE" in src
    assert "Notification.VISIBILITY_SECRET" in src

    # Idempotent start/stop guard
    assert "if (started) return" in src
    assert "if (!started) return" in src

    for bad in ["vivo", "originos", "Class.forName", "setAccessible(true)"]:
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
    test_bridge_uses_verified_apis_and_balanced_lifecycle()
    test_fail_closed_rro_gates_preserved()
    print("ALL Hardened Upstream Wiring Bridge & Fail-Closed Gate Tests PASSED (2/2)")

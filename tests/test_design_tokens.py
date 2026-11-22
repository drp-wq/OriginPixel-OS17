#!/usr/bin/env python3
"""Unit tests for OriginPixel OS 17 L1 Design System Tokens & Overlayable Map."""
import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path("/srv/originpixel-os17")
JSON_PATH = ROOT / "overlays/design-tokens/originpixel-tokens.json"
XML_PATH = ROOT / "overlays/design-tokens/values/originpixel_tokens.xml"
MAP_PATH = ROOT / "overlays/design-tokens/overlayable-map.json"


def test_tokens():
    data = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    assert data["schema_version"] == "1.0.0"
    assert "zero proprietary vivo" in data["clean_room_attestation"].lower()

    s = data["surfaces"]
    assert 0 < s["glass_low"]["blur_dp"] < s["glass_medium"]["blur_dp"] < s["glass_high"]["blur_dp"]
    assert s["reduced_transparency_fallback"]["blur_dp"] == 0
    assert s["reduced_transparency_fallback"]["alpha_light"] == 1.0
    assert s["reduced_transparency_fallback"]["alpha_dark"] == 1.0

    a = data["accessibility"]
    assert a["min_contrast_body"] >= 4.5
    assert a["min_contrast_large"] >= 3.0
    assert a["min_touch_target_dp"] >= 48

    tree = ET.parse(XML_PATH)
    dimens = {el.attrib["name"]: el.text for el in tree.getroot().findall("dimen")}
    assert dimens["op_radius_pill"] == f'{data["radii_dp"]["pill"]}dp'
    assert dimens["op_blur_glass_medium"] == f'{s["glass_medium"]["blur_dp"]}dp'
    assert dimens["op_min_touch_target"] == f'{a["min_touch_target_dp"]}dp'


def test_overlayable_map_integrity():
    omap = json.loads(MAP_PATH.read_text(encoding="utf-8"))
    assert len(omap["mappings"]) >= 4
    for entry in omap["mappings"]:
        status = entry["rro_compatibility_status"]
        ref = entry["verified_source_ref"]
        if status == "VERIFIED":
            assert ref and isinstance(ref, str), "VERIFIED RRO target must cite authoritative source ref"
        else:
            assert status == "UNVERIFIED_PENDING_G3_TREE", f"Unexpected status: {status}"
            assert ref is None, "UNVERIFIED RRO target must not carry fake source ref"


if __name__ == "__main__":
    test_tokens()
    test_overlayable_map_integrity()
    print("ALL L1 Design Token & Overlayable Map Tests PASSED (2/2)")

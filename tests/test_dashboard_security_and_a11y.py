#!/usr/bin/env python3
"""Security, iPad viewport/responsiveness, accessibility, and live endpoint tests."""
import json
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path("/srv/originpixel-os17")
HTML_FILE = ROOT / "dashboard/public/index.html"
JSON_FILE = ROOT / "dashboard/public/status.json"
BASE_URL = "http://10.77.0.1:8717"


def test_static_html_and_json():
    html = HTML_FILE.read_text(encoding="utf-8")
    assert 'name="viewport"' in html and "viewport-fit=cover" in html, "Missing iPad viewport meta"
    assert "color-scheme: light dark" in html, "Missing light/dark color-scheme"
    assert 'class="table-wrap"' in html, "Missing responsive table wrapper for iPad split-view"
    assert "<header>" in html and "<main>" in html, "Missing semantic landmarks"
    assert "<script" not in html.lower(), "Static dashboard must contain zero inline/external scripts"
    assert "http://" not in html and "https://" not in html, "Static dashboard must have zero external URLs"

    data = json.loads(JSON_FILE.read_text(encoding="utf-8"))
    assert data["virtual_harness"]["execution_mode"] == "LOCAL_VIRTUAL_MOCK_HARNESS_NOT_A_BOOTABLE_ROM"
    assert data["virtual_harness"]["overall_status"] == "PASS"
    assert "generated_utc" in data and data["generated_utc"].endswith("Z")


def test_live_wireguard_endpoint():
    with urllib.request.urlopen(f"{BASE_URL}/", timeout=5) as resp:
        assert resp.status == 200
        headers = {k.lower(): v for k, v in resp.headers.items()}
        assert "default-src 'none'" in headers.get("content-security-policy", "")
        assert headers.get("x-content-type-options") == "nosniff"
        assert headers.get("x-frame-options") == "DENY"
        assert headers.get("referrer-policy") == "no-referrer"
        assert headers.get("cache-control") == "no-store"

    with urllib.request.urlopen(f"{BASE_URL}/status.json", timeout=5) as resp:
        assert resp.status == 200
        live = json.loads(resp.read().decode("utf-8"))
        assert live["virtual_harness"]["overall_status"] == "PASS"

    req = urllib.request.Request(f"{BASE_URL}/", method="POST", data=b"")
    try:
        urllib.request.urlopen(req, timeout=5)
        raise AssertionError("POST / must return 405")
    except urllib.error.HTTPError as e:
        assert e.code == 405, f"Expected 405 on POST /, got {e.code}"


if __name__ == "__main__":
    test_static_html_and_json()
    test_live_wireguard_endpoint()
    print("ALL Dashboard Security, iPad Viewport & Live WireGuard Tests PASSED (2/2)")

#!/usr/bin/env python3
"""CI Smoke & Security Policy Tests for OriginPixel OS 17."""
import xml.etree.ElementTree as ET
from pathlib import Path
import yaml

ROOT = Path(__file__).resolve().parents[1]
CI_FILE = ROOT / ".github/workflows/ci-unit-tests.yml"
MANIFEST_FILE = ROOT / "manifests/originpixel_mustang.xml"


def test_ci_workflow_security_and_structure():
    data = yaml.safe_load(CI_FILE.read_text(encoding="utf-8"))
    assert data.get("permissions") == {"contents": "read"}, "Must enforce least-privilege read-only permissions"

    job = data["jobs"]["unit-and-schema-tests"]
    assert job.get("timeout-minutes", 999) <= 15, "CI job must have a strict timeout <= 15 min"

    for step in job["steps"]:
        if "uses" in step:
            ref = step["uses"].split("@", 1)[1].split()[0]
            assert len(ref) == 40 and all(c in "0123456789abcdef" for c in ref), (
                f"Action must be pinned to 40-char SHA: {step['uses']}"
            )
            if step["uses"].startswith("actions/checkout@"):
                assert step.get("with", {}).get("persist-credentials") is False, (
                    "actions/checkout must set persist-credentials: false"
                )


def test_local_manifest_xml():
    tree = ET.parse(MANIFEST_FILE)
    projects = tree.getroot().findall("project")
    assert len(projects) == 5, f"Expected 5 lineage-24.0 projects, found {len(projects)}"
    for p in projects:
        assert p.attrib.get("revision") == "lineage-24.0"


if __name__ == "__main__":
    test_ci_workflow_security_and_structure()
    test_local_manifest_xml()
    print("ALL CI Smoke & Manifest Tests PASSED (2/2)")

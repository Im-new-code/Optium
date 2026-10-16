#!/usr/bin/env python3
"""Create a separate OptiFine-owned-renderer performance candidate from the verified JAR."""
import json
import os
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROPS = ROOT / "gradle.properties"
props = {}
for line in PROPS.read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if line and not line.startswith("#") and "=" in line:
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()

version = props.get("mod_version")
if not version:
    raise SystemExit("FAIL: mod_version missing from gradle.properties")

compat_path = ROOT / "build" / "distributions" / f"Optium-{version}.jar"
performance_path = ROOT / "build" / "distributions" / f"Optium-performance-{version}.jar"
marker_name = "optium-performance-mode.marker"
if not compat_path.is_file():
    raise SystemExit(f"FAIL: verified compatibility JAR missing: {compat_path}")

with zipfile.ZipFile(compat_path, "r") as source:
    bad = source.testzip()
    if bad:
        raise SystemExit(f"FAIL: source JAR is corrupt at {bad}")
    names = source.namelist()
    if len(names) != len(set(names)):
        raise SystemExit("FAIL: source JAR contains duplicate entry paths")
    if marker_name in names:
        raise SystemExit("FAIL: compatibility JAR unexpectedly contains the performance marker")
    try:
        manifest = json.loads(source.read("fabric.mod.json").decode("utf-8"))
    except Exception as exc:
        raise SystemExit(f"FAIL: invalid source fabric.mod.json: {exc}")
    if manifest.get("id") != "optium" or manifest.get("version") != version:
        raise SystemExit("FAIL: compatibility JAR metadata does not match Optium/version")
    custom = manifest.setdefault("custom", {})
    if custom.get("fabric-renderer-indigo:force_compatibility") is not True:
        raise SystemExit("FAIL: compatibility JAR must retain Indigo compatibility mode")
    if custom.get("fabric-renderer-api-v1:contains_renderer") is True:
        raise SystemExit("FAIL: compatibility JAR must not claim exclusive renderer ownership")

    performance_manifest = json.loads(json.dumps(manifest))
    performance_custom = performance_manifest.setdefault("custom", {})
    performance_custom.pop("fabric-renderer-indigo:force_compatibility", None)
    performance_custom["fabric-renderer-api-v1:contains_renderer"] = True
    performance_bytes = (json.dumps(performance_manifest, indent=2, ensure_ascii=False) + "\n").encode("utf-8")

    performance_path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix="optium-performance-", suffix=".jar", dir=performance_path.parent)
    os.close(fd)
    try:
        with zipfile.ZipFile(temp_name, "w", allowZip64=True) as output:
            for info in source.infolist():
                data = performance_bytes if info.filename == "fabric.mod.json" else source.read(info.filename)
                output.writestr(info, data)
            output.writestr(marker_name, b"performance-renderer-profile=1\n", compress_type=zipfile.ZIP_STORED)
        with zipfile.ZipFile(temp_name, "r") as candidate:
            bad = candidate.testzip()
            if bad:
                raise SystemExit(f"FAIL: performance candidate ZIP validation failed at {bad}")
            names = candidate.namelist()
            if len(names) != len(set(names)):
                raise SystemExit("FAIL: performance candidate contains duplicate entry paths")
            check = json.loads(candidate.read("fabric.mod.json").decode("utf-8"))
            check_custom = check.get("custom", {})
            if check.get("id") != "optium" or check.get("version") != version:
                raise SystemExit("FAIL: performance candidate metadata does not match Optium/version")
            if check_custom.get("fabric-renderer-api-v1:contains_renderer") is not True:
                raise SystemExit("FAIL: performance candidate is missing contains_renderer=true")
            if "fabric-renderer-indigo:force_compatibility" in check_custom:
                raise SystemExit("FAIL: performance candidate still forces Indigo compatibility")
            if candidate.read(marker_name) != b"performance-renderer-profile=1\n":
                raise SystemExit("FAIL: performance candidate marker resource is incorrect")
            if "me/modmuss50/optium/gui/OptiumStatusScreen.class" in names:
                raise SystemExit("FAIL: status screen class must not be packaged")
        os.replace(temp_name, performance_path)
    finally:
        if os.path.exists(temp_name):
            os.remove(temp_name)

print(f"Performance candidate created: {performance_path.relative_to(ROOT)} ({performance_path.stat().st_size} bytes)")
print("Verified: exclusive renderer ownership, Indigo compatibility disabled, performance marker present, ZIP valid.")
print("Important: this candidate may break mods that require Indigo/Fabric custom block-model rendering.")

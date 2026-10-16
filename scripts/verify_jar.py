#!/usr/bin/env python3
"""Validate the actual remapped Optium JAR and stage one unambiguous artifact."""
import json
import re
import shutil
import struct
import sys
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
artifact_base = props.get("archives_base_name", "optium")
if not version:
    raise SystemExit("FAIL: mod_version missing from gradle.properties")

jar_path = ROOT / "build" / "libs" / f"{artifact_base}-{version}.jar"
if not jar_path.is_file():
    raise SystemExit(f"FAIL: expected remapped distribution JAR not found: {jar_path}")

errors = []
with zipfile.ZipFile(jar_path) as jar:
    bad = jar.testzip()
    if bad:
        errors.append(f"corrupt ZIP member: {bad}")
    names = jar.namelist()
    name_set = set(names)
    if len(name_set) != len(names):
        errors.append("duplicate ZIP entry paths found")

    try:
        manifest = json.loads(jar.read("fabric.mod.json").decode("utf-8"))
    except Exception as exc:
        raise SystemExit(f"FAIL: packaged fabric.mod.json is missing or invalid: {exc}")

    if manifest.get("id") != "optium":
        errors.append("packaged mod id is not 'optium'")
    if manifest.get("name") != "Optium":
        errors.append("packaged mod name is not 'Optium'")
    if manifest.get("version") != version:
        errors.append(f"packaged version {manifest.get('version')!r} != {version!r}")
    if manifest.get("license") != "MPL-2.0":
        errors.append("packaged licence declaration is not MPL-2.0")
    if "1.17.1" not in manifest.get("depends", {}).get("minecraft", []):
        errors.append("Minecraft 1.17.1 is not declared as supported")
    if not str(manifest.get("depends", {}).get("fabricloader", "")).startswith(">="):
        errors.append("Fabric Loader dependency is missing")

    icon_path = manifest.get("icon", "")
    try:
        icon_bytes = jar.read(icon_path)
        if icon_bytes != (ROOT / "src" / "main" / "resources" / icon_path).read_bytes():
            errors.append("packaged icon differs from the repository icon")
        if icon_bytes[:8] != b"\x89PNG\r\n\x1a\n" or len(icon_bytes) < 24:
            errors.append("packaged icon is not a valid PNG")
        else:
            width, height = struct.unpack(">II", icon_bytes[16:24])
            if width <= 0 or height <= 0:
                errors.append("packaged icon has invalid dimensions")
    except Exception as exc:
        errors.append(f"could not validate packaged icon {icon_path!r}: {exc}")

    for key, entries in manifest.get("entrypoints", {}).items():
        if not isinstance(entries, list):
            entries = [entries]
        for entry in entries:
            if not isinstance(entry, str) or "." not in entry:
                continue
            class_path = entry.replace(".", "/") + ".class"
            if class_path not in name_set:
                errors.append(f"entrypoint class missing from JAR: {entry}")

    embedded_jars = manifest.get("jars", [])
    for item in embedded_jars:
        embedded = item.get("file") if isinstance(item, dict) else None
        if not embedded or embedded not in name_set:
            errors.append(f"embedded dependency listed but not packaged: {embedded}")
            continue
        try:
            with zipfile.ZipFile(__import__("io").BytesIO(jar.read(embedded))) as nested:
                inner_bad = nested.testzip()
                if inner_bad:
                    errors.append(f"corrupt embedded dependency {embedded}: {inner_bad}")
        except Exception as exc:
            errors.append(f"invalid embedded dependency {embedded}: {exc}")

    # The project targets Java 8 bytecode for compatibility with its upstream
    # runtime bridge even when the CI compiler runs on Java 17.
    class_versions = {}
    for name in names:
        if not name.endswith(".class"):
            continue
        data = jar.read(name)
        if len(data) >= 8 and data[:4] == b"\xca\xfe\xba\xbe":
            major = struct.unpack(">H", data[6:8])[0]
            class_versions[major] = class_versions.get(major, 0) + 1
            if major > 52:
                errors.append(f"class targets newer than Java 8 (major {major}): {name}")

if errors:
    print("Optium packaged JAR verification failed:")
    for error in errors:
        print(" - " + error)
    raise SystemExit(1)

destination = ROOT / "build" / "distributions" / f"Optium-{version}.jar"
destination.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(jar_path, destination)
print(f"Packaged JAR verification passed: {jar_path.relative_to(ROOT)}")
print(f"Verified {len(names)} entries, {sum(n.endswith('.class') for n in names)} classes, embedded dependencies, entrypoints and icon.")
print(f"Java class-file versions: {class_versions}")
print(f"Staged single distributable: {destination.relative_to(ROOT)} ({destination.stat().st_size} bytes)")

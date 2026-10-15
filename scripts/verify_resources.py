#!/usr/bin/env python3
"""Consistency checks for Optium metadata, JSON, and mixin resources."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
JAVA_ROOTS = [ROOT / "src/main/java", ROOT / "src/shim/java"]
errors = []

def fail(message):
    errors.append(message)

manifest_path = RES / "fabric.mod.json"
try:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
except Exception as exc:
    print(f"FAIL: cannot parse {manifest_path}: {exc}")
    sys.exit(1)

if manifest.get("id") != "optium":
    fail("fabric.mod.json id must be 'optium'")
if manifest.get("name") != "Optium":
    fail("fabric.mod.json name must be 'Optium'")
if manifest.get("license") != "MPL-2.0":
    fail("MPL-2.0 license declaration is missing")
icon = RES / manifest.get("icon", "")
if not icon.is_file():
    fail(f"icon does not exist: {manifest.get('icon')}")
for config in manifest.get("mixins", []):
    if not (RES / config).is_file():
        fail(f"manifest mixin config does not exist: {config}")

def java_exists(qualified_name):
    relative = Path(*qualified_name.split(".")).with_suffix(".java")
    return any((base / relative).is_file() for base in JAVA_ROOTS)

for key, entries in manifest.get("entrypoints", {}).items():
    if not isinstance(entries, list):
        entries = [entries]
    for entry in entries:
        if isinstance(entry, str) and "." in entry and not java_exists(entry):
            fail(f"entrypoint {key} class is missing: {entry}")

sources = []
for base in JAVA_ROOTS:
    if base.exists():
        sources.extend(base.rglob("*.java"))
for source in sources:
    text = source.read_text(encoding="utf-8", errors="replace")
    for config in re.findall(r'"((?:optium)[^"]*mixins?\.json)"', text):
        if not (RES / config).is_file():
            fail(f"Java references missing mixin config {config}: {source.relative_to(ROOT)}")

for path in sorted(RES.rglob("*.json")):
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"invalid JSON {path.relative_to(ROOT)}: {exc}")
        continue
    if not isinstance(data, dict) or "package" not in data:
        continue
    package = data.get("package", "")
    names = []
    for key in ("mixins", "client", "server"):
        value = data.get(key, [])
        if isinstance(value, list):
            names.extend(x for x in value if isinstance(x, str))
    plugin = data.get("plugin")
    if isinstance(plugin, str):
        names.append(plugin)
    for name in names:
        if name.startswith("."):
            fail(f"relative mixin class name in {path.relative_to(ROOT)}: {name}")
            continue
        qualified = name if "." in name else (package + "." + name if package else name)
        if not java_exists(qualified):
            fail(f"mixin/plugin class missing: {qualified} (from {path.relative_to(ROOT)})")

for base in JAVA_ROOTS + [RES]:
    if base.exists():
        for path in base.rglob("*"):
            if not path.is_file() or path.suffix in (".class", ".png", ".jar"):
                continue
            try:
                text = path.read_text(encoding="utf-8", errors="replace")
            except Exception:
                continue
            if "optifabric:" in text or "optifabric.mixins.json" in text:
                fail(f"stale OptiFabric runtime identifier in {path.relative_to(ROOT)}")

if errors:
    print("Optium verification failed:")
    for error in errors:
        print(" - " + error)
    sys.exit(1)
print(f"Optium metadata/resource verification passed. Checked {len(sources)} Java sources.")

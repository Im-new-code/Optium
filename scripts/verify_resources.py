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
if "sodium" in manifest.get("conflicts", {}):
    fail("Sodium must be allowed to resolve so the early-riser renderer bypass can run")

setup_path = ROOT / "src/main/java/me/modmuss50/optium/mod/OptiumSetup.java"
title_mixin_path = ROOT / "src/main/java/me/modmuss50/optium/mixin/MixinTitleScreen.java"
try:
    setup_source = setup_path.read_text(encoding="utf-8")
    title_source = title_mixin_path.read_text(encoding="utf-8")
    for required in ('loader.isModLoaded("sodium")', 'loader.isModLoaded("iris")',
                     "disabledForExternalRenderer = true", "Skipping OptiFine loading"):
        if required not in setup_source:
            fail(f"external-renderer bypass is missing expected guard: {required}")
    if "OptiumSetup.disabledForExternalRenderer" not in title_source:
        fail("title-screen OptiFine UI must be guarded when an external renderer owns rendering")
except OSError as exc:
    fail(f"could not check external-renderer guard sources: {exc}")
icon = RES / manifest.get("icon", "")
if not icon.is_file():
    fail(f"icon does not exist: {manifest.get('icon')}")
for config in manifest.get("mixins", []):
    if not (RES / config).is_file():
        fail(f"manifest mixin config does not exist: {config}")

sources = []
for base in JAVA_ROOTS:
    if base.exists():
        sources.extend(base.rglob("*.java"))

# Optium intentionally has no custom status screen or per-tick keybinding hook.
# Keep this removed: the previous title-screen UI implementation crashed on the
# OptiFine runtime and adds no value to the renderer bridge.
for source in sources:
    if source.name in {"OptiumClient.java", "OptiumStatusScreen.java"}:
        fail(f"removed status UI must not be shipped: {source.relative_to(ROOT)}")
for base in JAVA_ROOTS + [RES]:
    if base.exists():
        for path in base.rglob("*"):
            if path.is_file() and path.suffix in {".java", ".json"}:
                try:
                    text = path.read_text(encoding="utf-8", errors="replace")
                except OSError:
                    continue
                if "key.optium.open_status" in text:
                    fail(f"stale status-screen keybinding reference: {path.relative_to(ROOT)}")

# Java top-level declarations, not filenames, are authoritative. Upstream has
# compatibility mixins where a package-private class intentionally lives in a
# differently named source file.
defined_types = set()
for source in sources:
    text = source.read_text(encoding="utf-8", errors="replace")
    package_match = re.search(r'^\s*package\s+([\w.]+)\s*;', text, re.M)
    package = package_match.group(1) if package_match else ""
    for match in re.finditer(
        r'\b(?:public\s+|protected\s+|private\s+|abstract\s+|final\s+|static\s+|sealed\s+|non-sealed\s+)*(?:class|interface|enum|record)\s+(\w+)',
        text,
    ):
        defined_types.add((package + "." if package else "") + match.group(1))

def type_exists(qualified_name):
    return qualified_name in defined_types

for key, entries in manifest.get("entrypoints", {}).items():
    if not isinstance(entries, list):
        entries = [entries]
    for entry in entries:
        if isinstance(entry, str) and "." in entry and not type_exists(entry):
            fail(f"entrypoint {key} class declaration is missing: {entry}")

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
        qualified = name if name.startswith("me.") else (package + "." + name if package else name)
        if not type_exists(qualified):
            fail(f"mixin/plugin class declaration missing: {qualified} (from {path.relative_to(ROOT)})")

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
print(f"Optium metadata/resource verification passed. Checked {len(sources)} Java sources and {len(defined_types)} declared types.")

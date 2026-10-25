#!/usr/bin/env python3
"""Verify that the OptiLithium port changes only intended metadata plus provenance."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import sys
import zipfile

PROVENANCE = "OPTILITHIUM-OPTIUM.txt"
EXPECTED_NAME = "OptiLithium for Optium"
EXPECTED_VERSION = "0.7.5-optium.1"

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def read_manifest(zf: zipfile.ZipFile) -> dict:
    return json.loads(zf.read("fabric.mod.json").decode("utf-8"))

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--input", required=True, type=Path, help="official Lithium 0.7.5 JAR")
    ap.add_argument("--patched", required=True, type=Path, help="repackaged OptiLithium JAR")
    args = ap.parse_args()
    try:
        with zipfile.ZipFile(args.input, "r") as original, zipfile.ZipFile(args.patched, "r") as result:
            original_list, result_list = original.namelist(), result.namelist()
            if len(set(original_list)) != len(original_list) or len(set(result_list)) != len(result_list):
                raise ValueError("duplicate ZIP entry names detected")
            old_names, new_names = set(original_list), set(result_list)
            if new_names - old_names != {PROVENANCE}:
                raise ValueError(f"unexpected added entries: {new_names-old_names}")
            if old_names - new_names:
                raise ValueError(f"original entries removed: {old_names-new_names}")

            old, new = read_manifest(original), read_manifest(result)
            if old.get("id") != "lithium" or new.get("id") != "lithium":
                raise ValueError("mod id must remain lithium")
            if not str(old.get("version", "")).startswith("0.7.5"):
                raise ValueError(f"input is not Lithium 0.7.5: {old.get('version')!r}")
            if new.get("version") != EXPECTED_VERSION or new.get("name") != EXPECTED_NAME:
                raise ValueError("unexpected repackaged mod name or version")
            if new.get("depends") != old.get("depends"):
                raise ValueError("game or loader dependencies were changed")
            if new.get("breaks", {}).get("optifabric") is not None or new.get("breaks"):
                raise ValueError(f"breaks metadata was not removed as expected: {new.get('breaks')!r}")
            mc = new.get("depends", {}).get("minecraft")
            supported = [mc] if isinstance(mc, str) else (mc or [])
            if "1.17.1" not in supported:
                raise ValueError(f"output does not target Minecraft 1.17.1: {mc!r}")
            if "Optium compatibility port" not in new.get("authors", []):
                raise ValueError("port attribution is missing")
            if old.get("license") != "LGPL-3.0-only" or new.get("license") != old.get("license"):
                raise ValueError("license metadata changed or is unexpected")
            if "lithium.mixins.json" not in new_names:
                raise ValueError("required Lithium mixin metadata is missing")

            license_name = next((n for n in ("LICENSE.txt", "LICENSE.md") if n in old_names), None)
            if not license_name:
                raise ValueError("upstream license file is missing")
            classes = {n for n in old_names if n.endswith(".class")}
            if not classes:
                raise ValueError("upstream contains no .class entries")

            changed = []
            for name in sorted(old_names):
                before, after = original.read(name), result.read(name)
                if before != after and name != "fabric.mod.json":
                    changed.append(name)
            if changed:
                raise ValueError("unexpected entry content changes: " + ", ".join(changed[:25]))
            for name in classes:
                if sha(original.read(name)) != sha(result.read(name)):
                    raise ValueError(f"class bytecode changed: {name}")
            if original.read(license_name) != result.read(license_name):
                raise ValueError("license contents changed")
            proof = result.read(PROVENANCE).decode("utf-8")
            if "Upstream JAR SHA-256:" not in proof or "breaks.optifabric" not in proof:
                raise ValueError("provenance record is incomplete")

            print("PASS: Lithium mod ID, 1.17.1 target, and dependencies verified.")
            print("PASS: breaks.optifabric removed.")
            print(f"PASS: all {len(classes)} class files are byte-for-byte unchanged.")
            print("PASS: all original entries except fabric.mod.json remain byte-identical.")
            print("PASS: Lithium mixin metadata and LGPL license retained; provenance added.")
            print(f"Input SHA-256:   {sha(args.input.read_bytes())}")
            print(f"Patched SHA-256: {sha(args.patched.read_bytes())}")
            return 0
    except (OSError, zipfile.BadZipFile, UnicodeDecodeError, json.JSONDecodeError, ValueError, KeyError) as exc:
        print(f"OptiLithium verification failed: {exc}", file=sys.stderr)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())

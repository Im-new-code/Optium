#!/usr/bin/env python3
"""Create a metadata-only OptiLithium repackaging from official Lithium 0.7.5."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import sys
import zipfile

REPO = "https://github.com/Im-new-code/Optium"
UPSTREAM_SOURCE = "https://github.com/CaffeineMC/lithium/tree/mc1.17.1-0.7.5"
PROVENANCE = "OPTILITHIUM-OPTIUM.txt"
OUTPUT_NAME = "OptiLithium for Optium"
OUTPUT_VERSION = "0.7.5-optium.1"
DEFAULTS = "assets/lithium/lithium-mixin-config-default.properties"

def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def read_json(zf: zipfile.ZipFile, name: str) -> dict:
    try:
        return json.loads(zf.read(name).decode("utf-8"))
    except KeyError as exc:
        raise ValueError(f"upstream JAR is missing {name}") from exc
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError(f"upstream {name} is not valid UTF-8 JSON") from exc

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--input", required=True, type=Path, help="official Lithium 0.7.5 JAR")
    ap.add_argument("--output", required=True, type=Path, help="output OptiLithium JAR")
    args = ap.parse_args()
    source, target = args.input.resolve(), args.output.resolve()
    if not source.is_file():
        ap.error(f"input JAR does not exist: {source}")
    if source == target:
        ap.error("input and output paths must be different")
    target.parent.mkdir(parents=True, exist_ok=True)

    try:
        with zipfile.ZipFile(source, "r") as upstream:
            listed = upstream.namelist()
            names = set(listed)
            if len(names) != len(listed):
                raise ValueError("upstream JAR contains duplicate ZIP entry names")
            manifest = read_json(upstream, "fabric.mod.json")
            if manifest.get("id") != "lithium":
                raise ValueError(f"expected mod id lithium, got {manifest.get('id')!r}")
            if not str(manifest.get("version", "")).startswith("0.7.5"):
                raise ValueError(f"expected Lithium 0.7.5, got {manifest.get('version')!r}")
            mc = manifest.get("depends", {}).get("minecraft")
            supported = [mc] if isinstance(mc, str) else (mc or [])
            if "1.17.1" not in supported:
                raise ValueError(f"upstream does not declare Minecraft 1.17.1 support: {mc!r}")
            breaks = manifest.get("breaks", {})
            if breaks != {"optifabric": "*"}:
                raise ValueError(f"unexpected breaks metadata; refusing unknown source variant: {breaks!r}")
            for required in ("lithium.mixins.json", DEFAULTS):
                if required not in names:
                    raise ValueError(f"upstream JAR is missing {required}")
            license_name = next((n for n in ("LICENSE.txt", "LICENSE.md") if n in names), None)
            if not license_name:
                raise ValueError("upstream JAR has no LICENSE.txt or LICENSE.md")

            patched = dict(manifest)
            patched["version"] = OUTPUT_VERSION
            patched["name"] = OUTPUT_NAME
            authors = list(patched.get("authors", []))
            if "Optium compatibility port" not in authors:
                authors.append("Optium compatibility port")
            patched["authors"] = authors
            contact = dict(patched.get("contact", {}))
            contact["sources"] = REPO
            contact["issues"] = REPO + "/issues"
            patched["contact"] = contact
            patched.pop("breaks", None)
            if patched.get("id") != "lithium" or patched.get("depends") != manifest.get("depends"):
                raise ValueError("patch unexpectedly changed mod id or dependencies")

            upstream_sha = sha256_file(source)
            proof = (
                "OptiLithium for Optium: compatibility repackaging of upstream Lithium\n\n"
                f"Upstream artifact: {source.name}\n"
                "Upstream version: Lithium 0.7.5\nMinecraft target: 1.17.1\n"
                f"Upstream source: {UPSTREAM_SOURCE}\nUpstream JAR SHA-256: {upstream_sha}\n\n"
                "Changes made:\n"
                "  1. fabric.mod.json: removed only the breaks.optifabric declaration.\n"
                f"  2. fabric.mod.json: set display name to {OUTPUT_NAME}, set version to {OUTPUT_VERSION}, "
                "added an Optium compatibility-port attribution, and pointed source/issues links to this repository.\n\n"
                "No .class files, mixin lists, access wideners, or Lithium default optimization settings are intentionally modified.\n"
                "The upstream LGPL-3.0-only license is retained.\n\n"
                "This packaging change is intended to allow testing with Optium in place of OptiFabric. "
                "It does not guarantee runtime compatibility with every OptiFine transformation. Test in a duplicate profile.\n"
            ).encode("utf-8")
            patched_manifest = (json.dumps(patched, ensure_ascii=False, indent=2) + "\n").encode("utf-8")

            with zipfile.ZipFile(target, "w") as out:
                out.comment = upstream.comment
                for info in upstream.infolist():
                    data = patched_manifest if info.filename == "fabric.mod.json" else upstream.read(info.filename)
                    out.writestr(info, data)
                info = zipfile.ZipInfo(PROVENANCE, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.external_attr = 0o100644 << 16
                out.writestr(info, proof, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

        print(f"Created: {target}")
        print(f"Upstream SHA-256: {upstream_sha}")
        print(f"Output SHA-256:   {sha256_file(target)}")
        print(f"Retained license: {license_name}")
        print("Lithium classes, mixin lists, and default optimization settings are unchanged.")
        return 0
    except (OSError, zipfile.BadZipFile, ValueError, KeyError) as exc:
        print(f"OptiLithium build error: {exc}", file=sys.stderr)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())

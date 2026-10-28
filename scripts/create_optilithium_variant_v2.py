#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, sys, zipfile
from pathlib import Path

REPO = "https://github.com/Im-new-code/Optium"
UPSTREAM_SOURCE = "https://github.com/CaffeineMC/lithium/tree/mc1.17.1-0.7.5"
PROVENANCE = "OPTILITHIUM-OPTIUM.txt"
OUTPUT_NAME = "OptiLithium for Optium"
OUTPUT_VERSION = "0.7.5-optium.1"

def sha(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as f:
        for b in iter(lambda: f.read(1024 * 1024), b""):
            h.update(b)
    return h.hexdigest()

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--input", required=True, type=Path)
    p.add_argument("--output", required=True, type=Path)
    a = p.parse_args()
    if not a.input.is_file() or a.input.resolve() == a.output.resolve():
        p.error("input must exist and input/output paths must differ")
    a.output.parent.mkdir(parents=True, exist_ok=True)
    try:
        with zipfile.ZipFile(a.input) as src:
            entries = src.infolist()
            names = [i.filename for i in entries]
            if len(names) != len(set(names)):
                raise ValueError("duplicate ZIP entries in upstream JAR")
            m = json.loads(src.read("fabric.mod.json"))
            if m.get("id") != "lithium" or not str(m.get("version", "")).startswith("0.7.5"):
                raise ValueError("input is not official Lithium 0.7.5")
            mc = m.get("depends", {}).get("minecraft")
            mc = [mc] if isinstance(mc, str) else (mc or [])
            if "1.17.1" not in mc:
                raise ValueError("input does not declare Minecraft 1.17.1")
            if m.get("breaks") != {"optifabric": "*"}:
                raise ValueError("unexpected breaks metadata; refusing to patch unknown variant")
            if "lithium.mixins.json" not in names or "LICENSE.txt" not in names:
                raise ValueError("upstream JAR is missing mixin configuration or license")
            new = dict(m)
            new["name"] = OUTPUT_NAME
            new["version"] = OUTPUT_VERSION
            new["authors"] = list(new.get("authors", []))
            if "Optium compatibility port" not in new["authors"]:
                new["authors"].append("Optium compatibility port")
            contact = dict(new.get("contact", {}))
            contact.update({"sources": REPO, "issues": REPO + "/issues"})
            new["contact"] = contact
            new.pop("breaks", None)
            proof = (
                "OptiLithium for Optium: compatibility repackaging of upstream Lithium\n\n"
                f"Upstream version: Lithium 0.7.5\nMinecraft target: 1.17.1\n"
                f"Upstream source: {UPSTREAM_SOURCE}\nUpstream JAR SHA-256: {sha(a.input)}\n\n"
                "Only fabric.mod.json metadata is changed: remove breaks.optifabric, identify this port, "
                "and set its source/issues links. All class files, mixin configuration, optimization "
                "logic, and license contents are retained byte-for-byte.\n"
                "This enables a runtime compatibility test; it does not guarantee OptiFine mixin compatibility.\n"
            ).encode()
            with zipfile.ZipFile(a.output, "w") as dst:
                dst.comment = src.comment
                for info in entries:
                    data = (json.dumps(new, indent=2, ensure_ascii=False) + "\n").encode() if info.filename == "fabric.mod.json" else src.read(info.filename)
                    dst.writestr(info, data)
                info = zipfile.ZipInfo(PROVENANCE, date_time=(1980,1,1,0,0,0))
                info.compress_type = zipfile.ZIP_DEFLATED
                dst.writestr(info, proof, compress_type=zipfile.ZIP_DEFLATED)
        print("Created", a.output)
        print("Upstream SHA-256:", sha(a.input))
        print("Output SHA-256:", sha(a.output))
    except (OSError, zipfile.BadZipFile, ValueError, KeyError, json.JSONDecodeError) as e:
        print("OptiLithium packaging failed:", e, file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

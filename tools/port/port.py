#!/usr/bin/env python3
"""Generate Tudur's Guns' Forge 1.20.1 sources from its Mojang-mapped Fabric project (1.21.11) and manage the
hand fixes. Adapted from tudursvehiclemod-forge's tools/port/port.py (same steps, same rules format).

    python tools/port/port.py generate --mojmap <TudursGuns project (Mojang mappings)>
    python tools/port/port.py makepatches        # after editing files under src/generated/java

generate:
  1. Builds a mapping "1.21.11 intermediary -> 1.20.1 Mojang names" (build_cross.py). Intermediary
     names are stable across Minecraft versions, so every class/method/field that still exists in
     1.20.1 gets its 1.20.1 name; anything that doesn't keeps its 1.21.11 name.
  2. Runs Loom's migrateClientMappings with that mapping on a copy of the Mojang project (main and
     client sources together), so names are rewritten with full type resolution.
  3. Drops the files listed in exclude.txt and applies rules.json (import redirects to the
     compatibility layer under src/forge/java, plus a few textual rewrites).
     The result is saved as .port/pristine (the base the patches are made against).
  4. Copies it to src/generated/java and applies tools/port/patches/**.patch.
  5. Copies the resources (minus the Fabric-only files) to src/generated/resources.

refresh: re-applies rules.json and the patches to the last migration output (after changing rules).

makepatches: writes one patch per file that differs from .port/pristine (and removes patches
of files that no longer differ). Edit src/generated/java freely, then run this before committing.
"""
import argparse
import difflib
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent.parent
WORK = PROJECT / ".port"  # outside build/ so `gradlew clean` keeps the migration output and the patch base
PRISTINE = WORK / "pristine"
GEN_JAVA = PROJECT / "src" / "generated" / "java"
GEN_RES = PROJECT / "src" / "generated" / "resources"
PATCHES = HERE / "patches"
GRADLE_HOME = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
M2 = Path.home() / ".m2" / "repository"


def log(msg):
    print(f"[port] {msg}", flush=True)


def fetch(url, dest):
    dest = Path(dest)
    if not dest.exists():
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(urllib.request.urlopen(url, timeout=120).read())
    return dest


# ------------------------------------------------------------------ mapping
def cross_mapping():
    """Builds the cross mapping jar and installs it into the local Maven repo; returns its coordinate."""
    base = GRADLE_HOME / "caches" / "fabric-loom" / "1.21.11"
    moj = sorted(base.glob("loom.mappings.*layered*/mappings.tiny"))
    if not moj:
        sys.exit("1.21.11 Mojang mappings not found in the Loom cache - build the Mojang project once first")
    maps = WORK / "mappings"
    manifest = json.loads(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", timeout=60).read())
    vjson = json.loads(urllib.request.urlopen(next(v["url"] for v in manifest["versions"] if v["id"] == "1.20.1"), timeout=60).read())
    pg = fetch(vjson["downloads"]["client_mappings"]["url"], maps / "client_1.20.1.txt")
    ijar = fetch("https://maven.fabricmc.net/net/fabricmc/intermediary/1.20.1/intermediary-1.20.1-v2.jar", maps / "intermediary-1.20.1-v2.jar")
    with zipfile.ZipFile(ijar) as z:
        (maps / "intermediary-1.20.1.tiny").write_bytes(z.read("mappings/mappings.tiny"))
    out = maps / "cross.tiny"
    subprocess.check_call([sys.executable, str(HERE / "build_cross.py"), str(moj[-1]), str(maps / "intermediary-1.20.1.tiny"), str(pg), str(out)])
    version = hashlib.sha1(out.read_bytes()).hexdigest()[:12]
    dest = M2 / "tvm" / "cross" / version
    dest.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(dest / f"cross-{version}.jar", "w") as z:
        z.write(out, "mappings/mappings.tiny")
    (dest / f"cross-{version}.pom").write_text(
        f"<project><modelVersion>4.0.0</modelVersion><groupId>tvm</groupId><artifactId>cross</artifactId><version>{version}</version></project>")
    return f"tvm:cross:{version}"


# ------------------------------------------------------------------ generate
def migrate(mojmap, coordinate):
    copy = WORK / "mojmap-copy"
    if copy.exists():
        shutil.rmtree(copy)
    shutil.copytree(mojmap, copy, ignore=shutil.ignore_patterns("build", ".gradle", ".git", "run", "tools", "sample_addon", "docs"))
    build = copy / "build.gradle"
    text = build.read_text(encoding="utf-8")
    if "// tvm-port: mavenLocal" not in text:
        text = re.sub(r"^repositories \{", "repositories {\n\tmavenLocal() // tvm-port: mavenLocal", text, count=1, flags=re.M)
        build.write_text(text, encoding="utf-8")
    merged, out = WORK / "merged", WORK / "migrated"
    for d in (merged, out):
        if d.exists():
            shutil.rmtree(d)
    for ss in ("main", "client"):
        shutil.copytree(copy / "src" / ss / "java", merged, dirs_exist_ok=True)
    gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"
    env = dict(os.environ)
    # Loom needs Java 21 while ForgeGradle 6 builds with Java 17: LOOM_JAVA_HOME picks the JDK for this step.
    if os.environ.get("LOOM_JAVA_HOME"):
        env["JAVA_HOME"] = os.environ["LOOM_JAVA_HOME"]
    log("running Loom migrateClientMappings (1.21.11 -> 1.20.1 names)")
    with open(WORK / "migrate.log", "w") as lf:
        rc = subprocess.call([gradlew, "--no-daemon", "--max-workers=1", "migrateClientMappings", "--mappings", coordinate,
                              "--input", str(merged), "--output", str(out)], cwd=copy, stdout=lf, stderr=subprocess.STDOUT,
                             shell=(os.name == "nt"), env=env)
    if rc:
        sys.exit(f"migration failed - see {WORK / 'migrate.log'}")
    return copy, out


def apply_rules(root):
    rules = json.loads((HERE / "rules.json").read_text(encoding="utf-8"))
    excludes = [l.strip() for l in (HERE / "exclude.txt").read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
    for pattern in excludes:
        for f in list(root.glob(pattern)):
            if f.is_dir():
                shutil.rmtree(f)
            else:
                f.unlink()
    for f in root.rglob("*.java"):
        text = f.read_text(encoding="utf-8")
        new = text
        for old, rep in rules["imports"].items():
            new = re.sub(r"\b" + re.escape(old) + r"\b", rep, new)
        rel = f.relative_to(root).as_posix()
        for r in rules["regex"]:
            # optional "files": glob(s) relative to the source root, for rewrites that only make sense in one file
            if "files" in r and not any(Path(rel).match(g) for g in ([r["files"]] if isinstance(r["files"], str) else r["files"])):
                continue
            new = re.sub(r["find"], r["replace"], new, flags=re.M)
        if new != text:
            f.write_text(new, encoding="utf-8")


def apply_patches():
    failed = []
    for p in sorted(PATCHES.rglob("*.patch")):
        rc = subprocess.call(["patch", "-p1", "--no-backup-if-mismatch", "-s", "--fuzz=3", "-i", str(p)], cwd=GEN_JAVA)
        if rc:
            failed.append(p.relative_to(PATCHES))
    return failed


def generate(mojmap):
    WORK.mkdir(parents=True, exist_ok=True)
    coordinate = cross_mapping()
    copy, migrated = migrate(Path(mojmap).resolve(), coordinate)
    return refresh(copy)


def refresh(copy=None):
    """Re-applies rules and patches to the last migration output (no Loom run)."""
    migrated = WORK / "migrated"
    copy = copy or WORK / "mojmap-copy"
    if PRISTINE.exists():
        shutil.rmtree(PRISTINE)
    shutil.copytree(migrated, PRISTINE)
    apply_rules(PRISTINE)
    if GEN_JAVA.exists():
        shutil.rmtree(GEN_JAVA)
    shutil.copytree(PRISTINE, GEN_JAVA)
    failed = apply_patches()
    if GEN_RES.exists():
        shutil.rmtree(GEN_RES)
    for ss in ("main", "client"):
        shutil.copytree(copy / "src" / ss / "resources", GEN_RES, dirs_exist_ok=True)
    for name in ("fabric.mod.json", "tudursguns.mixins.json", "tudursguns.client.mixins.json"):
        (GEN_RES / name).unlink(missing_ok=True)
    convert_resources(GEN_RES)
    n = sum(1 for _ in GEN_JAVA.rglob("*.java"))
    log(f"generated {n} source files; patches applied: {len(list(PATCHES.rglob('*.patch'))) - len(failed)}, failed: {len(failed)}")
    for f in failed:
        log(f"  PATCH FAILED: {f} (see the .rej file next to the source)")
    return not failed


# 1.21 data folder names -> 1.20.1 (singular -> plural)
DATA_DIRS = {"recipe": "recipes", "loot_table": "loot_tables", "advancement": "advancements",
             "tags/item": "tags/items", "tags/block": "tags/blocks", "tags/entity_type": "tags/entity_types",
             "tags/fluid": "tags/fluids", "tags/game_event": "tags/game_events"}


def _ingredient(v):
    """1.21.2+ ingredient ("minecraft:stick", "#minecraft:planks" or a list) -> 1.20.1 JSON object(s)."""
    if isinstance(v, str):
        return {"tag": v[1:]} if v.startswith("#") else {"item": v}
    if isinstance(v, list):
        return [_ingredient(x) for x in v]
    return v


def convert_resources(root):
    """Rewrites the copied 1.21.11 resources for 1.20.1: data folder names and the recipe JSON shape.
    assets/<ns>/items/*.json (1.21.4 item definitions) are left in place - 1.20.1 ignores them and uses
    models/item/*.json, which the project already ships."""
    convert_item_definitions(root)
    data = root / "data"
    if not data.exists():
        return
    for ns in data.iterdir():
        for old, new in DATA_DIRS.items():
            src = ns / old
            if src.is_dir():
                dst = ns / new
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copytree(src, dst, dirs_exist_ok=True)
                shutil.rmtree(src)
        for f in (ns / "recipes").rglob("*.json") if (ns / "recipes").exists() else []:
            r = json.loads(f.read_text(encoding="utf-8"))
            res = r.get("result")
            if isinstance(res, dict) and "id" in res:
                res["item"] = res.pop("id")
                comps = res.pop("components", None) or {}
                dyed = comps.pop("minecraft:dyed_color", None)
                if dyed is not None:
                    # Forge's crafting result "nbt": 1.20.1 keeps the dye colour in display.color
                    res["nbt"] = {"display": {"color": dyed["rgb"] if isinstance(dyed, dict) else dyed}}
                # This mod's own components are kept in the stack's NBT under their id (compat DataComponentType),
                # so they carry over as Forge's crafting result "nbt" unchanged.
                own = {k: comps.pop(k) for k in list(comps) if k.startswith("tudursguns:")}
                if own:
                    res.setdefault("nbt", {}).update(own)
                if comps:
                    log(f"  recipe {f.name}: result components {sorted(comps)} dropped (no 1.20.1 equivalent)")
            if "key" in r:
                r["key"] = {k: _ingredient(v) for k, v in r["key"].items()}
            if "ingredients" in r:
                r["ingredients"] = [_ingredient(v) for v in r["ingredients"]]
            for k in ("ingredient", "base", "addition", "template"):
                if k in r:
                    r[k] = _ingredient(r[k])
            f.write_text(json.dumps(r, indent=2), encoding="utf-8")


def _plain_model(node):
    """The model a 1.21.4 item definition shows without any components (its on_false branch), or None."""
    if not isinstance(node, dict):
        return None
    t = node.get("type")
    if t == "minecraft:model":
        return node.get("model")
    if t == "minecraft:condition":
        return _plain_model(node.get("on_false"))
    return None


def convert_item_definitions(root):
    """1.20.1 finds an item's model at models/item/<item>.json. Where an item definition
    (assets/<ns>/items/<item>.json) names another model (a block's, say) and there's no such file,
    write one that inherits it. Special models are drawn by forge.client.SpecialItemModels."""
    assets = root / "assets"
    if not assets.exists():
        return
    for f in assets.glob("*/items/*.json"):
        ns = f.parent.parent.name
        target = assets / ns / "models" / "item" / f.name
        if target.exists():
            continue
        model = _plain_model(json.loads(f.read_text(encoding="utf-8")).get("model"))
        if model and model != f"{ns}:item/{f.stem}":
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps({"parent": model}, indent=2) + "\n", encoding="utf-8")
            log(f"  item model {ns}:{f.stem} -> {model}")


def makepatches():
    if not PRISTINE.exists():
        sys.exit("no .port/pristine - run generate first")
    seen = set()
    for f in sorted(GEN_JAVA.rglob("*.java")):
        rel = f.relative_to(GEN_JAVA).as_posix()
        base = PRISTINE / rel
        a = base.read_text(encoding="utf-8").splitlines(keepends=True) if base.exists() else []
        b = f.read_text(encoding="utf-8").splitlines(keepends=True)
        if a == b:
            continue
        diff = "".join(difflib.unified_diff(a, b, f"a/{rel}" if a else "/dev/null", f"b/{rel}", n=3))
        out = PATCHES / (rel + ".patch")
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(diff, encoding="utf-8")
        seen.add(out)
    for p in list(PATCHES.rglob("*.patch")):
        if p not in seen:
            p.unlink()
    log(f"{len(seen)} patch file(s)")


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["generate", "refresh", "makepatches"])
    ap.add_argument("--mojmap")
    a = ap.parse_args()
    if a.command == "generate":
        if not a.mojmap:
            sys.exit("--mojmap is required")
        sys.exit(0 if generate(a.mojmap) else 1)
    if a.command == "refresh":
        sys.exit(0 if refresh() else 1)
    makepatches()

#!/usr/bin/env python3
"""Build a Yarn-style tiny v2 file ("intermediary" -> "named") whose names are 1.20.1 Mojang names,
keyed by 1.21.11 intermediary, so Loom's migrateMappings in the 1.21.11 Mojang project rewrites
sources to 1.20.1 names. Intermediary names are stable across versions; anything that no longer
exists in 1.20.1 keeps its 1.21.11 name (and fails to compile, to be handled by hand).

build_cross.py <1.21.11 mojmap tiny (official/intermediary/named)> <1.20.1 intermediary tiny>
               <1.20.1 client mappings (proguard)> <out.tiny>
"""
import re
import sys

PRIM = {"void": "V", "boolean": "Z", "byte": "B", "char": "C", "short": "S", "int": "I", "long": "J", "float": "F", "double": "D"}


def load_proguard(path):
    """obf class -> (named, {(obf member name, kind): named})"""
    classes = {}
    named_to_obf = {}
    cur = None
    for line in open(path, encoding="utf-8"):
        if line.startswith("#"):
            continue
        if not line.startswith(" "):
            m = re.match(r"(\S+) -> (\S+):", line)
            named, obf = m.group(1), m.group(2)
            cur = {"named": named.replace(".", "/"), "members": {}}
            classes[obf.replace(".", "/")] = cur
            named_to_obf[named] = obf.replace(".", "/")
        else:
            s = line.strip()
            m = re.match(r"(?:\d+:\d+:)?(\S+) (\S+?)(\((.*)\))?(?::\d+:\d+)? -> (\S+)$", s)
            if not m:
                continue
            ret, name, args, obf = m.group(1), m.group(2), m.group(4), m.group(5)
            if m.group(3) is None:
                cur["members"].setdefault(("f", obf), []).append((name, ret, None))
            else:
                cur["members"].setdefault(("m", obf), []).append((name, ret, args))
    return classes, named_to_obf


def jdesc(t, named_to_obf):
    dims = 0
    while t.endswith("[]"):
        dims += 1
        t = t[:-2]
    d = PRIM.get(t) or "L" + named_to_obf.get(t, t.replace(".", "/")) + ";"
    return "[" * dims + d


def load_intermediary(path):
    """official class -> (intermediary, [(kind, official desc, official name, intermediary name)])"""
    out = {}
    cur = None
    with open(path, encoding="utf-8") as f:
        f.readline()
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "c":
                cur = [p[2], []]
                out[p[1]] = cur
            elif len(p) >= 5 and p[0] == "" and p[1] in ("m", "f"):
                cur[1].append((p[1], p[2], p[3], p[4]))
    return out


def main():
    moj1211, int1201, pg1201, out = sys.argv[1:5]
    pg, named_to_obf = load_proguard(pg1201)
    inter = load_intermediary(int1201)
    cls1201 = {}      # intermediary class -> 1.20.1 named
    mem1201 = {}      # intermediary member name -> 1.20.1 named
    for obf, (icls, members) in inter.items():
        pc = pg.get(obf)
        if not pc:
            continue
        cls1201[icls] = pc["named"]
        for kind, odesc, oname, iname in members:
            cands = pc["members"].get((kind, oname), [])
            if kind == "f":
                if cands:
                    mem1201[iname] = cands[0][0]
                continue
            for name, ret, args in cands:
                d = "(" + "".join(jdesc(a, named_to_obf) for a in (args.split(",") if args else [])) + ")" + jdesc(ret, named_to_obf)
                if d == odesc:
                    mem1201[iname] = name
                    break
    off2int = {}
    with open(moj1211, encoding="utf-8") as f:
        ns = f.readline().rstrip("\n").split("\t")[3:]
        ii0 = ns.index("intermediary")
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "c":
                off2int[p[1]] = p[1:][ii0]
    def to_int(desc):
        return re.sub(r"L([^;]+);", lambda m: "L" + off2int.get(m.group(1), m.group(1)) + ";", desc)
    lines = ["tiny\t2\t0\tintermediary\tnamed"]
    kept = renamed = 0
    with open(moj1211, encoding="utf-8") as f:
        ns = f.readline().rstrip("\n").split("\t")[3:]
        ii, ni = ns.index("intermediary"), ns.index("named")
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "c":
                i, n = p[1:][ii], p[1:][ni]
                lines.append(f"c\t{i}\t{cls1201.get(i, n)}")
                if i in cls1201:
                    renamed += 1
                else:
                    kept += 1
            elif len(p) > 3 and p[0] == "" and p[1] in ("m", "f"):
                names = p[3:]
                i, n = names[ii], names[ni]
                # descriptors are in the first namespace (official); the target file needs intermediary
                # descriptors - Loom re-derives them, so keep the member only by name+official desc mapping:
                lines.append(f"\t{p[1]}\t{to_int(p[2])}\t{i}\t{mem1201.get(i, n)}")
    open(out, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print(f"classes mapped to 1.20.1: {renamed}, kept (no 1.20.1 class): {kept}, members known in 1.20.1: {len(mem1201)}")


if __name__ == "__main__":
    main()

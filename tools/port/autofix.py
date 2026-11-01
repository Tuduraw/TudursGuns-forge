#!/usr/bin/env python3
"""Mechanical fixes driven by javac errors (run after generate, before makepatches).

    python tools/port/autofix.py <gradle compile log>

Currently: Java 17 rejects `expr instanceof T t` when expr's static type is already T (Java 21 accepts
it). Each such site becomes `((Object) expr) instanceof T t`.
"""
import re
import sys
from pathlib import Path

ERR = re.compile(r"^(?!\s)(.+?\.java):(\d+): error: expression type .* is a subtype of pattern type .*$")


def expression_start(text, end):
    """Start of the expression ending right before `end` (identifiers, dots, calls, indexing)."""
    depth = 0
    i = end - 1
    while i >= 0:
        c = text[i]
        if c in ")]>":
            depth += 1
        elif c in "([<":
            if depth == 0:
                return i + 1
            depth -= 1
        elif depth == 0 and not (c.isalnum() or c in "_$."):
            return i + 1
        i -= 1
    return 0


def main(log):
    lines = Path(log).read_text(encoding="utf-8", errors="replace").splitlines()
    edits = {}
    for i, line in enumerate(lines):
        m = ERR.match(line)
        if m:
            caret = lines[i + 2].find("^")
            edits.setdefault(m.group(1), set()).add((int(m.group(2)), caret))
    n = 0
    for path, sites in edits.items():
        src = Path(path).read_text(encoding="utf-8").split("\n")
        for lineno, col in sorted(sites, key=lambda s: (s[0], -s[1])):
            text = src[lineno - 1]
            if text.lstrip().startswith("instanceof ") and lineno >= 2:
                # expression on the previous line
                prev = src[lineno - 2].rstrip()
                start = expression_start(prev, len(prev))
                src[lineno - 2] = prev[:start] + "((Object) " + prev[start:] + ")"
                n += 1
                continue
            k = text.find(" instanceof ", max(0, col - 1))
            if k < 0:
                continue
            start = expression_start(text, k)
            src[lineno - 1] = text[:start] + "((Object) " + text[start:k] + ")" + text[k:]
            n += 1
        Path(path).write_text("\n".join(src), encoding="utf-8")
    print(f"[autofix] instanceof sites wrapped: {n}")


if __name__ == "__main__":
    main(sys.argv[1])

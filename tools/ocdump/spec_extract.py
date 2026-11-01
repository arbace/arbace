#!/usr/bin/env python3
"""Extract the ```forms blocks of doc/ocaml/SPEC.md into entries for bin/repr-ocaml spec-check.

usage: spec_extract.py SPEC.md OUTDIR
An entry starts at a line ';; KIND: SOURCE' (';; | MORE' continues the source); lines before the first
header make a forms-only entry (only read). Per entry NNN: NNN.kind, NNN.src, NNN.ml (the source wrapped
by its kind so that ocdump parses it), NNN.forms (the forms text); NNN.mli for intf kinds.
"""
import re, sys, os

KINDS = "impl|intf|item|sigitem|expr|pat|type|module|modtype"
HEAD = re.compile(r"^;; (%s): ?(.*)$" % KINDS)
CONT = re.compile(r"^;; \| ?(.*)$")
WRAP = {
    "impl": "{}", "intf": "{}", "item": "{}", "sigitem": "{}",
    "expr": "let _ = {}", "pat": "let {} = ()", "type": "type _t = {}",
    "module": "module _ = {}", "modtype": "module type _T = {}",
}

def entries_of(text):
    blocks = re.findall(r"^```forms\n(.*?)^```$", text, re.S | re.M)
    entries = []  # (kind or None, src, forms)
    for b in blocks:
        cur = None
        pre = []
        for line in b.split("\n"):
            m = HEAD.match(line)
            c = CONT.match(line)
            if m:
                if cur is not None:
                    entries.append(cur)
                cur = [m.group(1), m.group(2), []]
            elif c and cur is not None and not cur[2]:
                cur[1] += "\n" + c.group(1)
            elif cur is not None:
                cur[2].append(line)
            else:
                pre.append(line)
        if cur is not None:
            entries.append(cur)
        if "".join(pre).strip():
            entries.append([None, None, pre])
    return blocks, entries

def main():
    spec, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    text = open(spec, encoding="utf-8").read()
    blocks, entries = entries_of(text)
    for i, (kind, src, forms) in enumerate(entries):
        n = "%03d" % i
        open(os.path.join(out, n + ".forms"), "w", encoding="utf-8").write("\n".join(forms) + "\n")
        if kind is None:
            continue
        open(os.path.join(out, n + ".kind"), "w").write(kind + "\n")
        open(os.path.join(out, n + ".src"), "w", encoding="utf-8").write(src + "\n")
        ext = "mli" if kind in ("intf", "sigitem") else "ml"
        open(os.path.join(out, n + "." + ext), "w", encoding="utf-8").write(WRAP[kind].format(src) + "\n")
    print("%d entries (%d with sources) from %d blocks" % (
        len(entries), sum(1 for e in entries if e[0]), len(blocks)))

if __name__ == "__main__":
    main()

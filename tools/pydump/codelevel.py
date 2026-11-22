#!/usr/bin/env python3
"""The round trip's level 3 by CPython (bin/repr-python roundtrip, live; doc/python/PYTHON-SURVEY.md, "As built: the code
level without positions"): the code compiled from a file's ast without positions, normalized by tools/pydump/pydump.py's
norm_code (also 3.15's: pydump315.py reuses it). The user's decision of 2026-11-22 (doc/JOURNAL.md, "the user's decisions on the
burst's by-design and counting questions"): level 3 compares what the forms record, and mode :lines records no positions,
while CPython 3.14's code generation depends on the lines a node spans (where a NOP stays, a superinstruction fuses, an
exception range or a jump falls). So every node's lineno, col_offset, end_lineno and end_col_offset are 1, 0, 1 and 0
before compile(), for the original and for its print alike. A separate script, so that pydump.py, whose digest keys every
record of the python-pydump packs, stays as it is (its CODE column, positions included, is the records' and the oracle's).

usage:
  codelevel.py --base DIR --tsv IN --out OUT
      IN's rows (pydump.py tree's TSV: REL STATUS AST CODE MARSHAL SHA NODES SECONDS RSS) written to OUT with the CODE
      column of each STATUS ok row replaced by the digest of DIR/REL's code without positions ("compile-error: MSG" when
      compile() rejects the tree); the other rows as they are
  codelevel.py --base DIR --list FILE --rows OUT
      each file of the list as a row REL STATUS AST CODE (STATUS ok or error; AST ast.dump's digest, as pydump.py's):
      the negative control's mutated prints

Every file is read as data: nothing of it is imported or run (compile() only compiles).
"""
import ast
import hashlib
import importlib.util
import os
import sys
import warnings

POSITIONS = (("lineno", 1), ("col_offset", 0), ("end_lineno", 1), ("end_col_offset", 0))


def load_helper(path):
    spec = importlib.util.spec_from_file_location("pydump_helper", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def without_positions(tree):
    """The tree with every node's positions the same: line 1, columns 0 (a node without attributes keeps none)"""
    for n in ast.walk(tree):
        for a, v in POSITIONS:
            if a in n._attributes:
                setattr(n, a, v)
    return tree


def code_level(helper, data, name):
    """Level 3's fact: the SHA-256 of the normalized code of the file's ast without positions"""
    try:
        co = compile(without_positions(ast.parse(data, name)), name, "exec", dont_inherit=True)
    except (SyntaxError, ValueError) as e:
        return "compile-error: " + str(getattr(e, "msg", e)).replace("\t", " ").replace("\n", " ")
    return hashlib.sha256(repr(helper.norm_code(co)).encode("utf-8", "surrogateescape")).hexdigest()


def opt(args, name, default=None):
    return args[args.index(name) + 1] if name in args else default


def main():
    sys.setrecursionlimit(20000)
    a = sys.argv[1:]
    if "--base" not in a or not (("--tsv" in a and "--out" in a) or ("--list" in a and "--rows" in a)):
        raise SystemExit(__doc__)
    helper = load_helper(os.path.join(os.path.dirname(os.path.abspath(__file__)), "pydump.py"))
    base = opt(a, "--base")
    warnings.simplefilter("ignore")
    if "--tsv" in a:
        with open(opt(a, "--tsv"), encoding="utf-8") as f, open(opt(a, "--out"), "w", encoding="utf-8") as o:
            for line in f:
                c = line.rstrip("\n").split("\t")
                if len(c) > 3 and c[1] == "ok":
                    rel = c[0]
                    try:
                        c[3] = code_level(helper, open(os.path.join(base, rel), "rb").read(), rel)
                    except RecursionError:
                        c[3] = "helper-error: recursion"
                o.write("\t".join(c) + "\n")
    else:
        with open(opt(a, "--list"), encoding="utf-8") as f, open(opt(a, "--rows"), "w", encoding="utf-8") as o:
            for rel in f.read().split("\n"):
                if not rel:
                    continue
                data = open(os.path.join(base, rel), "rb").read()
                try:
                    tree = ast.parse(data, rel)
                except (SyntaxError, ValueError):
                    o.write("%s\terror\t-\t-\n" % rel)
                    continue
                o.write("%s\tok\t%s\t%s\n" % (rel, hashlib.sha256(ast.dump(tree).encode("utf-8", "surrogateescape")).hexdigest(),
                                              code_level(helper, data, rel)))


if __name__ == "__main__":
    main()

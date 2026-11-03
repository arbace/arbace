#!/usr/bin/env python3
"""The Python corpora's measures (doc/python/SPEC.md §15, test/python/corpora.edn), for bin/repr-python.

usage: python3 -I -S -B corpus.py VERB BASE PATH... [--exclude PATH]...

Run isolated (-I), on the pinned interpreter; it reads the files named, never imports them. PATH is a file or a
directory under BASE; the selected files are the .py files under the PATHs, recursively, less those under an
--exclude PATH, sorted by their path relative to BASE (C locale order, bytes).

  count     one line: FILES LINES DIGEST: the selected files, their lines (newline bytes, as wc -l counts them)
            and the SHA-256 of `sha256sum`'s lines of the selected files ("SHA256  RELPATH\\n", sorted), the
            digest of the corpus's content
  tests     one line: MODULES CLASSES METHODS: the PATHs' entries named test_* (a module test_x.py or a
            package test_x/; for a file PATH, the file itself when so named), and, over the selected files, the
            classes (at any depth) that define a test method and those methods: a def or async def directly in a
            class body whose name starts with "test", as unittest's default loader selects them by name (static:
            a method inherited by several TestCase classes counts once, subTest and load_tests are not seen)
  rejected  the selected files ast.parse rejects, one "RELPATH<TAB>CLASS<TAB>MSG" line each, then the files it
            accepts and ast.parse(type_comments=True) rejects, as "RELPATH<TAB>type-comments<TAB>MSG"
  list      the selected files, one relative path a line
  costs     TSV... (bin/repr-python roundtrip's per-file stage files: REL STATUS ... SECONDS RSS as their last two
            columns): one Clojure map per stage, {:files :seconds :mean :p50 :p95 :max :max-file :peak-kib}
  samples   the selected files each parsed by ast.parse (plain and with type_comments=True) and compiled; then
            the node classes of the oracle's ast (spec-check proves them Python.asdl's) that no sample's tree
            holds, but Interactive, Expression and FunctionType (the other parse modes' tops) and the
            deprecated aliases; one line per failure or uncovered class, then a summary; exit 1 on any
"""
import ast
import hashlib
import os
import sys
import warnings


def select(base, paths, excludes):
    out = []
    ex = [os.path.normpath(e) for e in excludes]
    for p in paths:
        full = os.path.join(base, p)
        if os.path.isfile(full):
            cands = [os.path.normpath(p)]
        else:
            cands = []
            for d, ds, fs in os.walk(full):
                for f in fs:
                    if f.endswith(".py"):
                        cands.append(os.path.normpath(os.path.relpath(os.path.join(d, f), base)))
        for c in cands:
            if not any(c == e or c.startswith(e + os.sep) for e in ex):
                out.append(c)
    return sorted(set(out), key=lambda s: s.encode("utf-8", "surrogateescape"))


def costs(paths):
    out = []
    for p in paths:
        rows = [l.split("\t") for l in open(p, encoding="utf-8").read().split("\n") if l]
        secs = sorted((float(r[-2]), r[0]) for r in rows)
        n = len(secs)
        if not n:
            out.append('{:stage "%s" :files 0}' % os.path.basename(p))
            continue
        tot = sum(s for s, _ in secs)
        q = lambda f: secs[min(n - 1, int(f * n))][0]
        peak = max(int(r[-1]) for r in rows)
        out.append('{:stage "%s" :files %d :seconds %.2f :mean %.4f :p50 %.4f :p95 %.4f :max %.4f :max-file "%s" :peak-kib %d}'
                   % (os.path.basename(p).rsplit(".", 1)[0], n, tot, tot / n, q(0.5), q(0.95), secs[-1][0], secs[-1][1], peak))
    print("[" + "\n ".join(out) + "]")


def main():
    args = sys.argv[1:]
    if args[0] == "costs":
        costs(args[1:])
        return
    verb, base = args[0], args[1]
    paths, excludes = [], []
    i = 2
    while i < len(args):
        if args[i] == "--exclude":
            excludes.append(args[i + 1])
            i += 2
        else:
            paths.append(args[i])
            i += 1
    files = select(base, paths, excludes)
    warnings.simplefilter("ignore")
    if verb == "count":
        lines = 0
        h = hashlib.sha256()
        for f in files:
            b = open(os.path.join(base, f), "rb").read()
            lines += b.count(b"\n")
            h.update(("%s  %s\n" % (hashlib.sha256(b).hexdigest(), f)).encode("utf-8", "surrogateescape"))
        print(len(files), lines, h.hexdigest())
    elif verb == "tests":
        modules = 0
        for p in paths:
            full = os.path.join(base, p)
            if os.path.isfile(full):
                modules += os.path.basename(p).startswith("test_")
            else:
                for e in os.listdir(full):
                    if e.startswith("test_") and (e.endswith(".py") or os.path.isdir(os.path.join(full, e))):
                        modules += 1
        classes = methods = 0
        for f in files:
            try:
                tree = ast.parse(open(os.path.join(base, f), "rb").read())
            except (SyntaxError, ValueError):
                continue
            for n in ast.walk(tree):
                if isinstance(n, ast.ClassDef):
                    k = sum(1 for b in n.body if isinstance(b, (ast.FunctionDef, ast.AsyncFunctionDef))
                            and b.name.startswith("test"))
                    if k:
                        classes += 1
                        methods += k
        print(modules, classes, methods)
    elif verb == "rejected":
        for f in files:
            b = open(os.path.join(base, f), "rb").read()
            try:
                ast.parse(b)
            except (SyntaxError, ValueError) as e:
                print("%s\t%s\t%s" % (f, type(e).__name__, getattr(e, "msg", str(e))))
                continue
            try:
                ast.parse(b, type_comments=True)
            except SyntaxError as e:
                print("%s\ttype-comments\t%s" % (f, e.msg))
    elif verb == "list":
        for f in files:
            print(f)
    elif verb == "samples":
        bad = 0
        seen = set()
        for f in files:
            b = open(os.path.join(base, f), "rb").read()
            try:
                tree = ast.parse(b, f, type_comments=True)
                ast.parse(b, f)
                compile(b, f, "exec", dont_inherit=True)
            except (SyntaxError, ValueError) as e:
                print("FAIL %s: %s: %s" % (f, type(e).__name__, e))
                bad += 1
                continue
            seen.update(type(n).__name__ for n in ast.walk(tree))
        skip = {"Interactive", "Expression", "FunctionType",
                "AugLoad", "AugStore", "ExtSlice", "Index", "Param", "Suite", "slice"}
        kinds = sorted(n for n in dir(ast) if isinstance(getattr(ast, n), type)
                       and issubclass(getattr(ast, n), ast.AST) and getattr(ast, n)._fields is not None
                       and not getattr(ast, n).__subclasses__() and n not in skip)
        missing = [k for k in kinds if k not in seen]
        for k in missing:
            print("UNCOVERED %s" % k)
        print("samples: %d files, %d failing; %d of %d node classes covered" % (len(files), bad,
              len(kinds) - len(missing), len(kinds)))
        if bad or missing:
            sys.exit(1)
    else:
        raise SystemExit("corpus.py: unknown verb " + verb)


if __name__ == "__main__":
    main()

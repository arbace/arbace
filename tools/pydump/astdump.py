"""The reference of repr/python's ast check (bin/repr-python ast-check; doc/python/PYTHON-SURVEY.md, "As built: PY9
(errors, pyi)"): each file of a list parsed by CPython's compile() with PyCF_ONLY_AST, as pyi's compile() asks for it,
and its answer written in tools/pyi/ast_json.py's form (the node as JSON, "AST:" before it), an error as the compiler's
answer ("TYPE: MSG" and, after NULs, the line, offset, end line, end offset, p and the text), the answer pyi's _ast module rebuilds nodes from
(go/arbace/py/builtins_ast_parse.go). The native answer (arbace.repr.python.astjson) must be the same text.

    astdump.py --base DIR --list FILE --out DIR [--mode exec|eval|single] [--flags N]

Every file is read as data: nothing of it is imported or run."""

import ast
import json
import os
import sys


def enc(x):
    if isinstance(x, ast.AST):
        d = {"_": type(x).__name__}
        for f in x._fields:
            if hasattr(x, f):
                d[f] = enc(getattr(x, f))
        for a in x._attributes:
            if hasattr(x, a):
                d[a] = getattr(x, a)
        return d
    if isinstance(x, list):
        return [enc(i) for i in x]
    if x is None or isinstance(x, (bool, str)):
        return x
    if isinstance(x, int):
        return {"$": "int", "v": str(x)}
    if isinstance(x, float):
        return {"$": "float", "v": x.hex()}
    if isinstance(x, complex):
        return {"$": "complex", "r": x.real.hex(), "i": x.imag.hex()}
    if isinstance(x, bytes):
        return {"$": "bytes", "v": x.hex()}
    if x is Ellipsis:
        return {"$": "..."}
    raise TypeError("astdump: no encoding for %r" % type(x).__name__)


def answer(src, filename, mode, flags):
    """tools/pyi/ast_json.py's answer"""
    try:
        tree = compile(src, filename, mode, flags | ast.PyCF_ONLY_AST, dont_inherit=True)
    except SyntaxError as e:
        # the compiler's answer (arbace.python.run's form): the class, the message, the location, p and the text
        loc = [e.lineno, e.offset, e.end_lineno, e.end_offset]
        cls = type(e).__name__ if type(e).__name__ in ("IndentationError", "TabError") else "SyntaxError"
        return (cls + ": " + str(e.msg) + "".join("\0" + ("" if v is None else str(v)) for v in loc) + "\0p"
                + ("" if e.text is None else "\0" + e.text))
    except (ValueError, TypeError) as e:
        return "%s: %s" % (type(e).__name__, e)
    return "AST:" + json.dumps(enc(tree), ensure_ascii=True, separators=(",", ":"))


def main(argv):
    base = argv[argv.index("--base") + 1]
    lst = argv[argv.index("--list") + 1]
    out = argv[argv.index("--out") + 1]
    mode = argv[argv.index("--mode") + 1] if "--mode" in argv else "exec"
    flags = int(argv[argv.index("--flags") + 1]) if "--flags" in argv else 0
    sys.setrecursionlimit(10000)
    for rel in open(lst, encoding="utf-8").read().split("\n"):
        if not rel:
            continue
        data = open(os.path.join(base, rel), "rb").read()
        try:
            # pyi's compile() passes the source as text (str), as the runtime holds it
            src = data.decode("utf-8", "surrogateescape")
            r = answer(src, "<unknown>", mode, flags)
        except Exception as e:  # the helper's own failure, recorded
            r = "HelperError: %s: %s" % (type(e).__name__, e)
        dst = os.path.join(out, rel + ".json")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(dst, "w", encoding="utf-8", newline="\n") as f:
            f.write(r)


if __name__ == "__main__":
    main(sys.argv)

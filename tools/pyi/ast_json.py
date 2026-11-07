"""compile()'s PyCF_ONLY_AST for pyi (bin/pyi; arbace.python.run's "ast:" modes; PY6h, queue 200): the source on stdin
parsed by the host's CPython 3.14 (the reader until PY9's native one, as tools/pydump/pydump.py is) with compile()'s own
arguments, the tree printed as JSON for the runtime to rebuild as _ast nodes (go/arbace/py's builtins_ast_parse.go):
a node {"_": CLASS, FIELD: VALUE ..., ATTRIBUTE: INT ...}, a list an array, None, bools and str as JSON's; other
constants tagged: {"$": "int", "v": DECIMAL}, {"$": "float", "v": HEX}, {"$": "complex", "r": HEX, "i": HEX},
{"$": "bytes", "v": HEX}, {"$": "..."}. The answer is "AST:" and the JSON, or "SyntaxError: MSG" and, after NULs, the
line, offset, end line and end offset (arbace.python.run's form).

    python3 -I ast_json.py FILENAME MODE FLAGS FEATURE_VERSION OPTIMIZE < SOURCE
"""
import ast
import json
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
    raise TypeError("ast_json: no encoding for %r" % type(x).__name__)


def main():
    filename, mode, flags, feature, optimize = sys.argv[1:6]
    src = sys.stdin.buffer.read().decode("utf-8", "surrogateescape")
    try:
        tree = compile(src, filename, mode, int(flags) | ast.PyCF_ONLY_AST, dont_inherit=True,
                       _feature_version=int(feature), optimize=int(optimize))
    except SyntaxError as e:
        loc = [e.lineno, e.offset, e.end_lineno, e.end_offset]
        sys.stdout.write("SyntaxError: " + str(e.msg) + "".join("\0" + ("" if v is None else str(v)) for v in loc))
        return
    except (ValueError, TypeError) as e:
        sys.stdout.write("%s: %s" % (type(e).__name__, e))
        return
    sys.stdout.write("AST:" + json.dumps(enc(tree), ensure_ascii=True, separators=(",", ":")))


main()

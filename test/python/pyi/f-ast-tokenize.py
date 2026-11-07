# PY9 (queue 201): ast.parse, compile() of ASTs and tokenize on the native reader, as CPython gives them
import ast
import io
import tokenize

SOURCES = [
    "x = 1 + 2 * y\n",
    "def f(a, /, b=2, *c, d, **e) -> int:\n    'doc'\n    return a\n",
    "class C(B, metaclass=M):\n    @dec\n    async def g(self):\n        async with a as b:\n            await x\n",
    "s = 'a' 'b' u'c' + b'\\x00\\xff' + f'{x!r:>{w}} {y=}' + rb'\\d'\n",
    "n = [0x1F, 0o17, 0b1, 1_000, 1.5e-3, 3j, 1e400, .5, ...]\n",
    "match p:\n    case [1, *r] | {'k': v, **kw}:\n        pass\n    case Point(x=0) if x:\n        pass\n    case None:\n        pass\n",
    "type T[U: int, *V, **W] = list[U]\n",
    "try:\n    pass\nexcept* (E, F) as e:\n    raise X from e\nfinally:\n    del a[1:2, ::3]\n",
    "lambda x, *, y=1: (x if y else (z := 3))\n",
    "t = t'{name!s} and {obj.attr:{spec}}'\n",
    "global g\nnonlocal_ = {k: v for k, v in d.items() if k}\nprint(*a, **b)\n",
]

ERRORS = [
    "x = (1,\n",
    "def f()\n    pass\n",
    "x = 1 +\n",
    "if x:\npass\n",
    "  x = 1\n",
    "print 'hello'\n",
    "f(x for x in y, 1)\n",
    "a, b += 1\n",
    "'\\x1'\n",
    "x = 0x\n",
    "def f(x, x): pass\n",
    "from __future__ import braces\n",
    "f'{'\n",
    "if x:\n\tpass\n        pass\n",
]

for src in SOURCES:
    print(ast.dump(ast.parse(src), include_attributes=True))
    print(ast.dump(ast.parse(src, type_comments=True)))

for src in ERRORS:
    try:
        ast.parse(src)
        print("parsed", repr(src))
    except SyntaxError as e:
        print(type(e).__name__, e.msg, e.lineno, e.offset, e.end_lineno, e.end_offset, repr(e.text))

print(ast.dump(ast.parse("1 + 2", mode="eval")))
print(ast.literal_eval("{'a': [1, 2.5, -3j, (None, True)], 'b': b'x'}"))
code = compile(ast.parse("result = 6 * 7"), "<ast>", "exec")
ns = {}
exec(code, ns)
print(ns["result"])

for src in ["x = (1,\n  2)\nif x:\n    'a\\\n'\n", "def f():\n    return f'{x!r}' # c\n", "a = 1\r\nb = 2"]:
    for t in tokenize.generate_tokens(io.StringIO(src).readline):
        print(tokenize.tok_name[t.type], repr(t.string), t.start, t.end, repr(t.line))
for t in tokenize.tokenize(io.BytesIO("# coding: latin-1\nx = 'é'\n".encode("latin-1")).readline):
    print(tokenize.tok_name[t.type], repr(t.string), t.start, t.end)
for bad in ["x = (1,\n", "if x:\n  a\n b\n"]:
    try:
        list(tokenize.generate_tokens(io.StringIO(bad).readline))
    except (tokenize.TokenError, SyntaxError) as e:
        print(type(e).__name__, e.args)

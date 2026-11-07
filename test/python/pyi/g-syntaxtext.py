# PY6-core: a compile() SyntaxError's type and text as CPython's: the parser's text as the tokenizer gives it, a
# compiler's error's text read from its file (None for a string), IndentationError and TabError kept (test_exceptions'
# test_string_source, test_fstring's test_not_closing_quotes)
import os

for s in ["x = (\n", "x = $\ny=1\n", "a\nz = f\"\"\"\nabc\n", "return 1\n", "def f():\n  x = 1\n   y\n", "x = 1 +\n",
          "if 1:\n\tx\n        y\n", "def f():\nreturn\n", 'return "ä"']:
    try:
        compile(s, "<s>", "exec")
    except SyntaxError as e:
        print(type(e).__name__, repr(e.msg), repr(e.text), e.lineno, e.offset, e.end_lineno, e.end_offset)
p = os.path.join(os.environ.get("TMPDIR", "."), "g-syntaxtext-%d.py" % os.getpid())
with open(p, "w") as f:
    f.write("x = 1\nreturn 2\n")
try:
    compile(open(p).read(), p, "exec")
except SyntaxError as e:
    print(type(e).__name__, repr(e.msg), repr(e.text), e.lineno, e.offset)
os.remove(p)

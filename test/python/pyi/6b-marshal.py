# PY6b: marshal (Python/marshal.c): every type at every version, references and loops, interned strs, the
# errors, dump and load on files, code objects (equal after a round trip, run, refused without allow_code)
import marshal, sys, io

vals = [0, 1, -1, 2**31 - 1, 2**31, -2**31, -2**31 - 1, 2**65, -(2**100) + 7, 1.5, -0.0, float("inf"), 1j, 2 + 3j,
        "", "abc", "Andr\xe8", "€\U0001f600", "x" * 300, b"", b"ab", (), (1, "a"), [1, [2]], {"a": 1, 2: (3,)},
        {1, 2, 3}, frozenset({"x"}), frozenset(), None, True, False, ..., StopIteration, slice(1, 2, 3)]
for v in vals:
    for ver in range(6):
        try:
            d = marshal.dumps(v, ver)
        except ValueError as e:
            print(ver, repr(v)[:30], "ValueError", e)
            continue
        w = marshal.loads(d)
        print(ver, repr(v)[:30], w == v and type(w) is type(v), len(d))
for bad in [b"0", b"f", b"l\x02\x00\x00\x00\x00\x00\x00\x00", b"\xdb\x01\x00\x00\x00r\x00\x00\x00\x00",
            b"\xbc\x01\x00\x00\x00r\x00\x00\x00\x00", b"\xbar\x00\x00\x00\x00NN", b"?", b"I\xfe\xdc\xba\x98\x76\x54\x32\x10", "T"]:
    try:
        r = marshal.loads(bad)
        print(repr(bad), "->", type(r).__name__, r[0] is r if isinstance(r, list) else r)
    except Exception as e:
        print(repr(bad), type(e).__name__, e)
a = []
a.append(a)
for v in range(6):
    try:
        b = marshal.loads(marshal.dumps(a, v))
        print(v, b[0] is b)
    except ValueError as e:
        print(v, e)
s = sys.intern("this is an interned string")
print(marshal.loads(marshal.dumps(s)) is s, marshal.loads(marshal.dumps(s, 2)) is s)
d = marshal.dumps(("hello", "dolly", None))
errs = set()
for i in range(len(d)):
    try:
        marshal.loads(d[:i])
    except EOFError as e:
        errs.add(str(e))
print(sorted(errs))
f = io.BytesIO()
marshal.dump([1, "x"], f)
marshal.dump(2.5, f)
f.seek(0)
print(marshal.load(f), marshal.load(f), f.tell())
for x in (1.0, 1j, b"0123456789", "0123456789"):
    class BadReader(io.BytesIO):
        def readinto(self, buf):
            n = super().readinto(buf)
            return n + 10**6 if n is not None and n > 4 else n
    try:
        marshal.load(BadReader(marshal.dumps(x)))
    except ValueError as e:
        print("ValueError", e)
print(marshal.dumps(5, 0), marshal.dumps(1.25, 1), marshal.dumps("a" * 3, 4)[1:])
try:
    marshal.dumps(([], memoryview(b"")), 5)
    marshal.dumps(frozenset([int]))
except ValueError as e:
    print("ValueError", e)


def g(x, *a, k=3, **kw):
    "doc"
    y = lambda: x
    return [i for i in a] + [k, y()]


c = g.__code__
c2 = marshal.loads(marshal.dumps(c))
print(c == c2, hash(c) == hash(c2), c2.co_name, sorted(c2.co_varnames), c2.co_argcount, c2.co_kwonlyargcount, c2.co_flags == c.co_flags)
print(marshal.loads(marshal.dumps((c,) * 3000))[5] == c)
for kw in ({"allow_code": False},):
    try:
        marshal.dumps(c, **kw)
    except ValueError as e:
        print(e)
    try:
        marshal.loads(marshal.dumps(c), **kw)
    except ValueError as e:
        print(e)
exec(marshal.loads(marshal.dumps(compile("def h(n):\n    'h doc'\n    return [i * i for i in range(n)]\nprint(h(4), h.__doc__)", "<s>", "exec"))))

# PY7, PY10's synthetic divergences: bytes' center padding, expandtabs, istitle; float.from_number and
# complex.from_number (3.14); reversed() of a dict and its views; range.count and range.index; next() of a finished
# generator with its return value; _operator._compare_digest
import _operator
x = b"Hello, World 12"
print(x.center(20, b"*"), bytearray(x).center(20, b"*"), b"ab".center(5), b"abc".center(6, b"-"))
print(b"a\tb\n\tc".expandtabs(4), bytearray(b"x\ty").expandtabs(), b"a\tb".expandtabs(tabsize=2))
print([t.istitle() for t in (b"Hello World", b"Hello world", b"HELLO", b"", b"1A", bytearray(b"Ab Cd"))])
class F(float):
    pass
print(float.from_number(3), float.from_number(2.5), float.from_number(True), type(F.from_number(2)).__name__)
print(complex.from_number(2), complex.from_number(1j), complex.from_number(2.5))
class I:
    def __index__(self):
        return 7
print(float.from_number(I()), complex.from_number(I()))
for f, v in ((float.from_number, "1"), (float.from_number, 1j), (complex.from_number, "1"), (float.from_number, None)):
    try:
        f(v)
    except TypeError as e:
        print("TypeError", e)
d = {"a": 1, "b": 2, "c": 3}
del d["b"]
d["d"] = 4
print(list(reversed(d)), list(reversed(d.keys())), list(reversed(d.values())), list(reversed(d.items())))
print(type(reversed(d)).__name__, type(reversed(d.items())).__name__)
it = reversed(d)
d["e"] = 5
try:
    next(it)
except RuntimeError as e:
    print("RuntimeError", e)
r = range(2, 20, 3)
print(r.count(5), r.count(6), r.count(5.0), r.count(True), range(0, 3).count(True), r.index(8), range(0, 5).index(3.0))
print(range(10**20, 10**20 + 10, 3).index(10**20 + 9))
for v in (6, 6.0, "a"):
    try:
        r.index(v)
    except ValueError as e:
        print("ValueError", e)
def h():
    yield 1
    return "value"
it = h()
next(it)
try:
    next(it)
except StopIteration as e:
    print("stop", e.value, e.args)
print(next(it, "default"))
print(_operator._compare_digest(b"ab", b"ab"), _operator._compare_digest("ab", "ac"),
      _operator._compare_digest(bytearray(b"a"), memoryview(b"a")), _operator._compare_digest(b"a", b"ab"))
for a, b in (("a", b"a"), ("\xe9", "\xe9"), (1, 1), (b"a", 2)):
    try:
        _operator._compare_digest(a, b)
    except TypeError as e:
        print("TypeError", e)
import hmac, secrets
print(hmac.compare_digest(b"k", b"k"), len(secrets.token_hex(4)))

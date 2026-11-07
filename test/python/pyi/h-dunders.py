# PY6h's REQUESTs: built-in types' number-protocol slot wrappers, __doc__ on instances, object's repr address,
# code attributes in dir(types.CodeType), writable cell_contents
import re, types
print(int.__add__(2, 3), (5).__radd__(1), int.__add__(2, 1.5), float.__mul__(2.0, 3), list.__add__([1], [2]), (2).__pow__(10, 7), int.__sub__(True, 1))
class I(int): pass
print(I(3) + 4, 4 + I(3), I(3).__add__(1), [1] * 2, 2 * [1], "a" + "b", "a%s" % 1)
class F(float):
    def __radd__(self, o): return "radd"
print(1 + F(2.0), F(2.0) + 1)
import operator; print(operator.add(1, 2), sum([1, 2.5]))
print(hasattr("x", "__doc__"), hasattr(1, "__doc__"))
class B: pass
print(bool(re.fullmatch(r"<__main__\.B object at 0x[0-9a-f]+>", repr(B()))))
print([n for n in ("co_flags", "co_name", "co_varnames") if n in dir(types.CodeType)])
def outer():
    x = 1
    def inner(): return x
    return inner
f = outer(); f.__closure__[0].cell_contents = 5; print(f())
from unittest import mock
import inspect
print(inspect.iscoroutinefunction(mock.AsyncMock()), inspect.iscoroutinefunction(mock.Mock()))
import dataclasses
@dataclasses.dataclass(slots=True)
class P:
    x: int
    def m(self): return __class__
print(P(1), P(1).m() is P, hasattr(P, "__dict__"))
print(ord(b"a"), ord(bytearray(b"\xff")), ord("\u20ac"))
class S(str): pass
print(ord(S("x")))
for x in (b"ab", "", 3):
    try: ord(x)
    except TypeError as e: print(e)

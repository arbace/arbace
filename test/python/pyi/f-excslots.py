# PY6a: __slots__ on exception classes, against CPython
class E(Exception):
    __slots__ = ('x',)
e = E(1); e.x = 5; print(e.x, e.args, hasattr(e, '__dict__'))
try:
    raise E('m')
except E as err:
    print(type(err).__name__, err)
class F(E):
    __slots__ = ('y',)
f = F(); f.y = 2; f.x = 3; print(f.x, f.y)

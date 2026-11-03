# PY6e's REQUESTs for PY6-core: what doctest -> pdb -> asyncio -> dataclasses and typing need at import, and
# OSError's two filenames; equal to CPython
import sys, dataclasses
def outer():
    x = 1
    def inner():
        return x
    return inner
c = outer().__closure__[0]
c.cell_contents = 5; print(c.cell_contents)
del c.cell_contents
try: c.cell_contents
except ValueError as e: print('ValueError', e)
class A: pass
class B: pass
class C(A): pass
C.__bases__ = (B,); print(C.__bases__, C.__mro__)
try: C.__bases__ = ()
except TypeError as e: print(e)
try: int.__bases__ = (object,)
except TypeError as e: print(e)
@dataclasses.dataclass(slots=True)
class P:
    x: int
    def m(self): return __class__
print(P(1), P.__slots__, P(1).m() is P)
print(OSError(2, 'No such file or directory', 'a', None, 'b'))
print(sys._get_cpu_count_config())
print(int.__add__(3, 4), int.__radd__(3, 4), type(int.__add__).__name__, (5).__sub__(2), int.__add__(1, 2.5), list.__iadd__([1], [2]))
import select
try: type(select.poll())()
except TypeError as e: print(e)
import multiprocessing.reduction

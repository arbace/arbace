# PY6a: type.__flags__'s bits (those pyi models), against CPython
# (OrderedDict and deque left out: CPython makes them C heap types, pyi has OrderedDict in Python and deque built in)
import abc, inspect, collections
MASK = (1 << 5) | (1 << 6) | (1 << 8) | (1 << 9) | (1 << 10) | (1 << 12) | (1 << 20) | (0xff << 24)
class A: pass
class B(int): pass
class C(list): pass
class D(dict): pass
class E(Exception): pass
class M(type): pass
class Abs(abc.ABC):
    @abc.abstractmethod
    def f(self): pass
class Con(Abs):
    def f(self): pass
for t in [object, int, bool, str, bytes, list, tuple, dict, set, frozenset, type, range, BaseException, ValueError,
          A, B, C, D, E, M, Abs, Con]:
    print(t.__name__, hex(t.__flags__ & MASK))
print(inspect.isabstract(Abs), inspect.isabstract(Con), inspect.isabstract(A))

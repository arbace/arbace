# PY6-core: _abc (Modules/_abc.c in Go): ABCMeta's registry and checks, and collections.abc's pattern flags
# (__abc_tpflags__, register's flag set on the class and its subclasses): test_patma's TestInheritance and the
# Sequence and Mapping subclasses of test_patma_184-186, 239, 240
import abc
import collections
import collections.abc


def seq_then_map(x):
    match x:
        case [*_]:
            return "seq"
        case {}:
            return "map"


class Seq(collections.abc.Sequence):
    __getitem__ = None

    def __len__(self):
        return 0


print(seq_then_map(Seq()), seq_then_map(collections.UserDict()), seq_then_map(collections.UserList()))


class C:
    pass


class M1(collections.UserDict, collections.abc.Sequence):
    pass


class M4(dict, collections.abc.Sequence, C):
    pass


class S1(collections.UserList, collections.abc.Mapping):
    pass


class S4(collections.UserList, dict, C):
    pass


print([seq_then_map(k()) for k in (M1, M4, S1, S4)])


class Parent:
    pass


class ChildPre(Parent):
    pass


collections.abc.Mapping.register(Parent)


class ChildPost(Parent):
    pass


print([seq_then_map(k()) for k in (Parent, ChildPre, ChildPost)])
print(isinstance(Parent(), collections.abc.Mapping), issubclass(ChildPost, collections.abc.Mapping))
print(bool(collections.abc.Sequence.__flags__ & 32), bool(collections.abc.Mapping.__flags__ & 64),
      bool(ChildPre.__flags__ & 64), "__abc_tpflags__" in collections.abc.Sequence.__dict__)


class A(abc.ABC):
    @abc.abstractmethod
    def f(self): ...

    @classmethod
    def __subclasshook__(cls, c):
        return True if hasattr(c, "quack") else NotImplemented


class B(A):
    def f(self):
        return 1


class Duck:
    quack = 1


print(sorted(A.__abstractmethods__), sorted(B.__abstractmethods__))
try:
    A()
except TypeError as e:
    print("TypeError", e)
print(B().f(), isinstance(Duck(), A), issubclass(int, A), isinstance(3, collections.abc.Hashable))
A.register(int)
print(issubclass(int, A), isinstance(5, A))
try:
    A.register(5)
except TypeError as e:
    print("TypeError", e)
try:
    B.register(A)
except RuntimeError as e:
    print("RuntimeError", e)
print(type(abc.get_cache_token()).__name__, abc.ABCMeta.__module__, A._abc_impl.__class__.__name__)
r, c, n, v = abc._get_dump(A)
print(len(r), type(v).__name__)

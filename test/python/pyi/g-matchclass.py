# PY6-core: a class pattern's isinstance is PyObject_IsInstance, a metaclass's __instancecheck__ deciding (test_patma's
# runtime-checkable protocols and test_regular_protocol)
from typing import Protocol, runtime_checkable
import collections.abc


@runtime_checkable
class P(Protocol):
    x: int
    y: int


@runtime_checkable
class Q(Protocol):
    __match_args__ = ('x', 'y')
    x: int
    y: int


class R(Protocol): ...


class A:
    def __init__(self, x, y):
        self.x, self.y = x, y


class B(A): ...


for cls in (A, B):
    match cls(1, 2):
        case P() as p:
            print("P", type(p).__name__, p.x, p.y)
    match cls(1, 2):
        case Q(x, y=y):
            print("Q", x, y)
match 1:
    case collections.abc.Hashable():
        print("hashable")
match [1]:
    case collections.abc.Sized():
        print("sized")
try:
    match 1:
        case R():
            pass
except TypeError as e:
    print("TypeError", e)

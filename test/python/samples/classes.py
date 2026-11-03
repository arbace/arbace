# Classes, type parameters and type aliases (doc/python/SPEC.md §7.1, §10).
class Meta(type):
    def __new__(mcls, name, bases, ns, **kw):
        ns["tag"] = kw.get("tag", "none")
        return super().__new__(mcls, name, bases, ns)


class Base:
    x: int = 0
    (y): str
    z: list[int]


class Box[T: (int, str) = int, **P = [int]](Base, metaclass=Meta, tag="box"):
    def __init__(self, v: T) -> None:
        self.v = v

    def __repr__(self):
        return f"Box({self.v!r})"


type Pair[K] = tuple[K, K]
type Alias = Box[int]


def first[*Ts](*a: *Ts) -> tuple[*Ts]:
    return a[:1]

b = Box(3)
print(b, Box.tag, Box.__type_params__, Pair.__value__, Alias.__name__)
print(first(1, 2), first.__type_params__)
print(Box.__mro__[1].__name__, Base.x, Base.__annotations__)
b.v += 1
del b.v
print(hasattr(b, "v"))

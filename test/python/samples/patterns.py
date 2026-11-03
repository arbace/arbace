# match and every pattern (doc/python/SPEC.md §9).
from dataclasses import dataclass


@dataclass
class Point:
    x: int
    y: int


def describe(cmd):
    match cmd:
        case {"op": "go", **rest}:
            return ("go", rest)
        case Point(x=0, y=_) | [None, True, *_] as whole:
            return ("origin or seq", whole)
        case [1, *rest] if rest:
            return ("one and", rest)
        case Point(1, y):
            return ("x is one", y)
        case -1 | 2.5 | "s" | False:
            return ("value", cmd)
        case str() | bytes():
            return ("text", cmd)
        case _:
            return ("other", cmd)


for c in [{"op": "go", "n": 1}, Point(0, 9), [None, True, 3], [1, 2], Point(1, 5), -1, "t", 7]:
    print(describe(c))

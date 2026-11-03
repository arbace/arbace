# Type comments and type: ignore (doc/python/SPEC.md §5.8), read with type_comments=True.
import os  # type: ignore[import]

x = []  # type: list[int]


def scale(a,  # type: int
          b,  # type: float
          ):
    # type: (...) -> float
    return a * b


for i in range(2):  # type: int
    x.append(i)
with open(os.devnull) as fh:  # type: object
    pass
print(x, scale(2, 1.5))

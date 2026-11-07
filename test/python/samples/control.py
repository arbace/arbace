# Statements and expressions: control flow, exceptions, with, imports, operators, comprehensions, slices
# (doc/python/SPEC.md §5, §6, §8).
import os.path as p, sys
from collections import OrderedDict as OD, deque
from math import *

xs = [3, 1, 2]
for i, x in enumerate(xs):
    if i == 0:
        continue
    elif x > 2:
        break
    else:
        pass
else:
    print("no break")

n = 3
while n:
    n -= 1
else:
    print("while done", n)

try:
    raise ValueError("v") from None
except TypeError, KeyError:
    pass
except ValueError as e:
    print("caught", e, e.__suppress_context__)
else:
    pass
finally:
    print("finally")

try:
    raise ExceptionGroup("g", [OSError(1), ValueError(2)])
except* OSError as eg:
    print("os", len(eg.exceptions))
except* ValueError:
    print("value")


class Ctx:
    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False


with Ctx() as c, Ctx():
    assert c is not None, "ctx"
with (Ctx() as d, Ctx() as e):
    pass

a, b, c, d, e, f, g = 7, 2, 3, 5, 6, 12, 4
r = -a // b ^ ~d | (e & f) % g << 1 >> 1
r2 = a / b * c - d + e ** 2 % 5
ok = not (a is not None and 0 < b <= 9 or c not in xs) or a in xs and b is b and a != b == 2 >= 1
print(r, r2, ok, +a)

m = [[1, 0], [0, 1]]
try:
    m @ m
except TypeError as err:
    print(type(err).__name__)

v = {"k": [xs[1:], (a, *xs)], **{"d": 1}}
s = {x * 2 for x in xs if x} | {0, 1}
dc = {k: w for k, w in v.items() if k}
ge = sum(x * x for x in xs)
lc = [y for x in m for y in x if y]
print(v, sorted(s), dc, ge, lc, xs[::2], xs[:], xs[-1:0:-1], m[0][1:2])
print(xs[1] if xs else None, p.join("a", "b"), OD(a=1), deque([1]), floor(pi), sys.maxsize > 0)
del xs[0], v["k"]
print(xs, v)

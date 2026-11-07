# math (PY6g): floor and ceil by __floor__/__ceil__ (math_floor), comb and perm of arbitrary ints, prod's keyword-only start, nextafter's steps
import math, struct
class FloorIsNone(float):
    __floor__ = None
class Floor:
    def __floor__(self): return 42
class FloatFloor(float):
    def __floor__(self): return 41
def tr(f, *a, **k):
    try: return repr(f(*a, **k))
    except Exception as e: return '%s: %s' % (type(e).__name__, e)
print(tr(math.floor, Floor()), tr(math.floor, FloatFloor(2.5)), tr(math.floor, FloorIsNone(3.5)), tr(math.floor, object()), tr(math.ceil, 2.1), tr(math.floor, 10**30))
n = 2**1000
print([tr(math.comb, *a) for a in ((n, 0), (n, 1), (n, n - 1), (1, 2**1000), (10, 3), (True, False), (-1, 1), (1, -2**1000), (10, 1.0), (10,))])
print([tr(math.perm, *a) for a in ((5,), (5, None), (n, 2), (3, 5), (10, 3), (-1,), (10, 1.0))])
print(tr(math.prod, [1, 2, 3], start=2), tr(math.prod, [10, 20], 1), tr(math.prod, [2, 3], start='ab'), tr(math.prod, []), tr(math.prod, iterable=[1]))
h = lambda x: struct.pack('<d', x).hex() if isinstance(x, float) else x
for a, k in (((1.0, float('inf')), {}), ((1.0, -float('inf')), {'steps': 3}), ((-0.0, 0.0), {}), ((0.0, -0.0), {'steps': 2}), ((5e-324, -0.0), {}), ((1.0, 2.0), {'steps': 0}), ((1.0, 2.0), {'steps': 2**70}), ((-1.0, 1.0), {'steps': 10})):
    try: print(h(math.nextafter(*a, **k)))
    except Exception as e: print(type(e).__name__, e)
print(tr(math.nextafter, 1.0, 2.0, steps=-1), tr(math.nextafter, 1.0, 2.0, x=1))

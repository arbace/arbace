# math.fsum as math_fsum (PY6g): the intermediate overflow, zero partials dropped, ASSIGN_DOUBLE's ints
import math, struct
h = lambda x: struct.pack('<d', x).hex()
for v in ([], [0.0], [-0.0], [1e100, 1.0, -1e100, 1e-100, 1e50, -1.0, -1e50], [2.0**53, -0.5, -2.0**-54], [1e16, 1., 1e-16], [0.1] * 10, [1.0, math.inf], [math.nan, 1.0], [1, 2**60, True], [1.7**i for i in range(200)]):
    print(h(math.fsum(v)))
for v in ([1e308, 1e308], [math.inf, -math.inf], [10**1000], ['x'], 5):
    try: print(math.fsum(v))
    except Exception as e: print(type(e).__name__, e)

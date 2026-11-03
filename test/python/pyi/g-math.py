# math (PY6g): musl's log2, log10, exp2, cbrt, erf, erfc; CPython's gamma and lgamma, loghelper, vector_norm, sumprod, fma; FUNC1D's messages; bits compared
import math, struct
def h(x): return struct.pack('<d', x).hex() if isinstance(x, float) else repr(x)
vals = [0.0, -0.0, 1e-320, 1e-300, 1e-20, 1e-5, 0.1, 0.5, 0.9, 1.0, 1.5, 2.0, 2.5, 3.0, 5.0, 7.5, 10.0, 23.0, 24.0, 100.5,
        141.0, 171.5, 180.0, 201.0, 1e10, 1e300, -0.5, -1.0, -1.5, -2.5, -10.25, -100.5, -170.5, -199.5, -201.5, -1e10,
        float('inf'), float('-inf'), float('nan'), 2**60, 10**400, -3, True]
for f in ('log2', 'log10', 'exp2', 'cbrt', 'erf', 'erfc', 'gamma', 'lgamma', 'log', 'log1p', 'sqrt', 'acos', 'acosh', 'atanh', 'sin', 'exp'):
    out = []
    for v in vals:
        try: out.append(h(getattr(math, f)(v)))
        except Exception as e: out.append('%s: %s' % (type(e).__name__, e))
    print(f, ' | '.join(out))
print(math.log(10**400, 10), math.log(8, 2), math.log2(2**1100), math.log10(10**500))
print(h(math.hypot(3, 4)), h(math.hypot(1e-310, 1e-311, 2e-310)), h(math.hypot(1.1, 2.2, 3.3)), math.hypot(), math.hypot(-2), math.hypot(1, float('nan'), float('inf')))
print(h(math.dist((1, 2.5), [3, 7])), h(math.dist([1e308, 1e308], [-1e308, -1e308])))
print(math.sumprod([1, 2, 3], [4, 5, 6]), h(math.sumprod([0.1] * 10, [0.3] * 10)), math.sumprod([1, 2.5, 3], [4, 5, 6.5]), math.sumprod([2**70, 1], [3, 1]), math.sumprod([], []))
print(h(math.fma(0.1, 10, -1)), math.fma(2, 3, 4))
for f in (lambda: math.fma(1e308, 10, 0), lambda: math.fma(float('inf'), 0, 1), lambda: math.sumprod([1], [1, 2]), lambda: math.dist([1], [1, 2]), lambda: math.log(-1), lambda: math.log(0)):
    try: print(f())
    except Exception as e: print(type(e).__name__, e)

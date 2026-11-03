# cmath (PY6g): Lib/test/mathdata/cmath_testcases.txt's 2,000-odd cases, polar, phase, rect and the rest, results as bits
import cmath, struct, os, sys
def h(z):
    if isinstance(z, complex): return struct.pack('<dd', z.real, z.imag).hex()
    if isinstance(z, float): return struct.pack('<d', z).hex()
    return repr(z)
lib = os.path.dirname(os.__file__)
n = bad = 0
out = []
tc = os.path.join(lib, 'test', 'mathdata', 'cmath_testcases.txt')
if not os.path.exists(tc):  # the oracle's own stdlib has no test package: the pinned tarball's, beside the checkout
    tc = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', '.tmp', 'repr-python', 'Python-3.14.10', 'Lib', 'test', 'mathdata', 'cmath_testcases.txt')
with open(tc) as f:
    for line in f:
        line = line.split('--')[0].strip()
        if not line or ':' in line.split('->')[0] and '->' not in line: continue
        if '->' not in line: continue
        lhs, rhs = line.split('->')
        parts = lhs.split()
        name, fn, a, b = parts[0], parts[1], float(parts[2]), float(parts[3])
        try: r = h(getattr(cmath, fn)(complex(a, b)))
        except Exception as e: r = type(e).__name__
        out.append(r)
print(len(out))

print("\n".join(out))
print('\n'.join(out[:0]))
vals = [0.0, -0.0, 1.0, -1.0, 0.5, 2.5, 1e-310, 1e300, 1e308, float('inf'), -float('inf'), float('nan'), 710.0, -710.0]
for fn in ('polar', 'phase', 'isfinite', 'isnan', 'isinf'):
    print(fn, ' '.join(h(getattr(cmath, fn)(complex(a, b))) for a in vals for b in vals[:6]))
for a in vals:
    for b in vals:
        try: r = h(cmath.rect(a, b))
        except Exception as e: r = type(e).__name__
        print(r, end=' ')
print()
print(h(cmath.log(8, 2)), h(cmath.log(1j, 1+1j)), cmath.isclose(1+1j, 1+1.0000000001j), cmath.pi, cmath.e, cmath.tau, cmath.inf, cmath.infj, cmath.nan, cmath.nanj)
class C:
    def __complex__(s): return 3+4j
class D:
    def __float__(s): return 2.0
print(cmath.sqrt(C()), cmath.sqrt(D()), cmath.sqrt(4), cmath.exp(True))
for f in (lambda: cmath.sqrt('x'), lambda: cmath.sqrt(), lambda: cmath.sqrt(1, 2), lambda: cmath.log(), lambda: cmath.log(1, 2, 3), lambda: cmath.rect(1), lambda: cmath.sqrt(z=1), lambda: cmath.isclose(1), lambda: cmath.isclose(1, 2, 3), lambda: cmath.log(0), lambda: cmath.log(1, 0), lambda: cmath.isclose(1, 1, rel_tol=-1)):
    try: f()
    except Exception as e: print(type(e).__name__, e)

# int's and float's __floor__, __ceil__, __trunc__ (PY6g's REQUEST for PY6-core)
import math
class I(int): pass
class F(float): pass
for n in ('__floor__', '__ceil__', '__trunc__'):
    print(n in int.__dict__, n in float.__dict__, getattr(5, n)(), type(getattr(True, n)()), getattr(-2.5, n)(), getattr(I(3), n)(), getattr(F(-1.5), n)())
print(math.trunc(1), math.trunc(True), math.trunc(I(4)), math.floor(F(2.5)), math.ceil(I(7)), math.trunc(-0.5))
for f in (lambda: math.trunc(object()), lambda: (2.5).__floor__(1), lambda: math.floor(float('nan')), lambda: math.ceil(float('inf'))):
    try: f()
    except Exception as e: print(type(e).__name__, e)

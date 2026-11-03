# complex(x): __complex__ on the real part, the single argument's TypeError, the DeprecationWarnings (PY6g's REQUEST for PY6-core)
class Rect:
    def __init__(self, x, y): self.x, self.y = x, y
    def __complex__(self): return complex(self.x, self.y)
class Bad:
    def __complex__(self): return 5
print(complex(Rect(1, -0.0)), complex(Rect(2, 3), 1.5))
for a in ((Bad(),), (object(),), ('1+2j',)):
    try: print(complex(*a))
    except Exception as e: print(type(e).__name__, e)
import warnings
warnings.simplefilter('always')
class C(complex): pass
print(complex(1j)); print(complex(C(1j))); print(complex(1j, 2)); print(complex(2, 1j)); print(complex(C(2j), 1)); print(complex(Rect(1, 2)))

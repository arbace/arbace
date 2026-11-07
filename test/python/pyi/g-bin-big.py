# bin, oct and hex of any int (PY6g's REQUEST for PY6-core)
import math
m, e = math.frexp(1e-300)
print(m, e, int(math.ldexp(m, 53)))
t = 3 << 2000
print(len(bin(t)), len(bin(-t)), len(hex(t)), len(oct(t)))
print(max(len(bin(abs(t))) - 2 - 53, -1074 - 5))
tm, te = 0, 0
for x in [1e100, 1.0, -1e100, 1e-100]:
    mant, exp = math.frexp(x)
    mant, exp = int(math.ldexp(mant, 53)), exp - 53
    if te > exp:
        tm <<= te - exp; te = exp
    else:
        mant <<= exp - te
    tm += mant
print(tm.bit_length(), te)
class I:
    def __index__(self): return -2**70
print(bin(I()), hex(-2**63), oct(2**64))
for a in (1.5, 'x', None):
    try: bin(a)
    except Exception as e: print(type(e).__name__, e)

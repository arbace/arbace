# _random (PY6g): Modules/_randommodule.c's Mersenne Twister and seeding, bit for bit with CPython
import _random
r=_random.Random()
for a in (-1, 1.5, 2**64):
  try: r.getrandbits(a)
  except Exception as e: print(type(e).__name__, e)
for st in [(1.0,)*625, (2**32,)+(1,)*624, (2**64,)+(1,)*624,(1,)*624+(625,),(1,)*624+(-1,),(1,)*624+(2**63,),(1,)*624+(1.0,), (1,)*3, [1]]:
  try: r.setstate(st); print('ok', r.getrandbits(32))
  except Exception as e: print(type(e).__name__, e)
for f in (lambda: _random.Random(1,2), lambda: _random.Random(x=1), lambda: r.seed(1,2), lambda: r.seed([1])):
  try: f()
  except Exception as e: print(type(e).__name__, e)
class I(int):
  def __abs__(s): return 5
r.seed(I(-3)); a=r.random(); r.seed(3); print(a==r.random())
r.seed(-3); a=r.random(); r.seed(3); print(a==r.random())
r.seed(0); print(r.random(), r.getrandbits(100), r.getrandbits(32), r.getrandbits(1), r.getrandbits(0), r.getrandbits(64))
r.seed(2**200+12345); print(r.random())
r.seed(1.5); print(r.random())
r.seed("abc"); r.seed(None); r.seed()
print(_random.Random(5).random(), _random.Random.__module__, type(r).__mro__, _random.__doc__)
s=r.getstate(); x=r.random(); r.setstate(s); print(x==r.random(), len(s))
# MT19937's stream past one regeneration, and long getrandbits
r.seed(12345); print([r.getrandbits(32) for _ in range(700)][-3:], r.getrandbits(1000) % 10**12, r.getrandbits(33))

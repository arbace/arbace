# PY6h: _random's stand-in (the Mersenne Twister as CPython seeds it)
import random
r = random.Random(12345)
print([r.random() for _ in range(3)], r.getrandbits(70), r.randrange(1000), r.choice("abcdef"))
s = r.getstate(); a = r.random(); r.setstate(s); print(a == r.random())
print(random.Random(2**100 + 7).random(), random.Random(-5).random(), random.Random(0).random())
l = list(range(10)); random.Random(1).shuffle(l); print(l)
print(random.Random(3).gauss(0, 1))

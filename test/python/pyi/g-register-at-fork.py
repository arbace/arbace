# random imported (PY6g): os.register_at_fork's checks, the seeded functions of random.py
import os, random
random.seed(42); print(random.random(), random.randrange(100), random.choice('abc'), random.sample(range(100), 5))
random.seed(42, version=1); print(random.random())
random.seed(b"x" * 3, version=1); print(random.random())
class R(random.Random): pass
print(R(7).random(), random.Random(7).random(), random.gauss(0, 1), random.uniform(1, 2), random.shuffle(l := list(range(10))) or l)
for kw in ({}, {'before': 1}, {'x': 1}):
    try: os.register_at_fork(**kw)
    except Exception as e: print(type(e).__name__, e)
try: os.register_at_fork(print)
except Exception as e: print(type(e).__name__, e)
print(os.register_at_fork(before=print, after_in_child=print))

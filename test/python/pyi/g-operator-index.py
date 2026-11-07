# operator.index is PyNumber_Index (_operator, PY6g's REQUEST for PY6-core)
import operator, random
for a in (3, True, 1.5, 'x', 2**70):
    try: print(repr(operator.index(a)), operator.__index__ is operator.index)
    except Exception as e: print(type(e).__name__, e)
for a in ((1.5,), (1, 2.5), (0,), (5, 1)):
    try: print(random.randrange(*a))
    except Exception as e: print(type(e).__name__, e)

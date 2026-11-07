# PY7: the C level's recursion check (_Py_EnterRecursiveCall): comparison, repr, a tuple's hash, isinstance's and
# issubclass's recursions raise RecursionError where they overflowed Go's stack (PY6i's 10 driver crashes); a nested
# structure of a few thousand levels still works; defaultdict's repr of a factory bound to itself (Py_ReprEnter), and a
# deque's and a defaultdict's subclass named in their repr
import copy, operator, sys
from collections import defaultdict, deque

def nest(n):
    t = ()
    for _ in range(n):
        t = (t,)
    return t

t = nest(3000)
print("3000 levels", hash(t) == hash(nest(3000)), len(repr(t)), t == nest(3000), isinstance(1, t))
t = nest(50_000)
for name, f in (("hash", hash), ("repr", repr), ("compare", lambda t: t == nest(50_000)), ("isinstance", lambda t: isinstance(1, t))):
    try:
        f(t)
        print(name, "no error")
    except RecursionError as e:
        print(name, "RecursionError", str(e).split(")")[-1])

for x in ([], {}, None):
    if x is None:
        x = ([],)
        x[0].append(x)
    elif isinstance(x, list):
        x.append(x)
    else:
        x['foo'] = x
    y = copy.deepcopy(x)
    for op in (operator.lt, operator.eq, operator.ne, operator.ge):
        try:
            op(y, x)
            print(type(x).__name__, op.__name__, "no error")
        except RecursionError as e:
            print(type(x).__name__, op.__name__, "RecursionError", str(e).endswith(" in comparison"))
        except TypeError as e:
            print(type(x).__name__, op.__name__, "TypeError", e)

def blowstack(fxn, arg, compare_to):
    tuple_arg = (compare_to,)
    while True:
        for _ in range(1000):
            tuple_arg = (tuple_arg,)
        fxn(arg, tuple_arg)
for f in (issubclass, isinstance):
    try:
        blowstack(f, str if f is issubclass else '', str)
    except RecursionError as e:
        print(f.__name__, "RecursionError", str(e).split(")")[-1])

class Failure(object):
    def __getattr__(self, attr):
        return (self, None)
class X:
    @property
    def __bases__(self):
        return (self, self, self)
class Raising:
    @property
    def __bases__(self):
        raise KeyError("bases")
old = sys.getrecursionlimit()
for o in (Failure(), X()):
    sys.setrecursionlimit(150)
    try:
        issubclass(o, int)
    except RecursionError as e:
        print(type(o).__name__, "RecursionError", str(e).split(")")[-1])
    finally:
        sys.setrecursionlimit(old)
try:
    issubclass(Failure(), (Raising(),))
except KeyError as e:
    print("bases raising", repr(e))
print(sys.getrecursionlimit() == old)

class sub(defaultdict):
    def __init__(self):
        self.default_factory = self._factory
    def _factory(self):
        return []
print(repr(sub()).replace(hex(id(0)), "")[:60])
class S(defaultdict):
    pass
d = S(list)
d[1] = d
print(repr(d))
class Q(deque):
    pass
q = Q([1], 5)
q.append(q)
print(repr(q))

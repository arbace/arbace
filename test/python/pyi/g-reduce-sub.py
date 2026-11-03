# PY6-core: object's methods take the instance itself for a class deriving from a built-in type (PY6g's F2):
# random.Random's and a tuple subclass's pickling and copying
import copy
import pickle
import random

r = random.Random(5)
print(r.__reduce__()[0].__name__, len(r.__reduce__()[2][1]))
print(r.__reduce_ex__(2)[0].__name__)
print(pickle.loads(pickle.dumps(r)).random() == random.Random(5).random())
print(copy.copy(r).random() == random.Random(5).random())


class T(tuple):
    pass


t = T((1, 2))
for p in range(2, 6):
    u = pickle.loads(pickle.dumps(t, p))
    print(p, type(u).__name__, u)
print(object.__reduce_ex__(t, 2)[0].__name__, type(copy.copy(t)).__name__)


class L(list):
    def __init__(self, *a):
        super().__init__(*a)
        self.x = 1


l = pickle.loads(pickle.dumps(L([1, 2]), 2))
print(type(l).__name__, l, l.x)
for v in [5, 1.5, 2j, "s", b"b", (1, 2), True]:
    print(type(v).__name__, v.__getnewargs__())


class S(str):
    pass


print(type(pickle.loads(pickle.dumps(S("ab"), 2))).__name__, copy.copy(S("ab")))

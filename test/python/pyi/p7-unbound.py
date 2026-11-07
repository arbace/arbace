# PY7: a built-in method taken from its type and called with no argument (method_check_args: dict.update() was a Go
# panic, test_ordered_dict's CPythonBuiltinDictTests.test_update); dict.fromkeys a class method (cls() filled by its
# __setitem__; it was a plain built-in bound to an instance)
import collections
for f in (dict.update, list.append, str.join, set.add, list.sort, dict.get, collections.deque.append, dict.fromkeys,
          object.__init__, int.__add__):
    try:
        f()
    except TypeError as e:
        print(e)
print({}.fromkeys([1, 2]), dict.fromkeys("ab", 0), {"x": 1}.fromkeys(()))
class D(dict):
    def __setitem__(self, k, v):
        super().__setitem__(k, [v])
d = D.fromkeys([3], 4)
print(type(d).__name__, d)
print(collections.OrderedDict.fromkeys("xy"), collections.defaultdict.fromkeys([5]))
for args in ((), (1, 2, 3)):
    try:
        dict.fromkeys(*args)
    except TypeError as e:
        print(e)
try:
    dict.fromkeys([1], value=2)
except TypeError as e:
    print(e)
od = dict()
od.update(self=23)
print(od)

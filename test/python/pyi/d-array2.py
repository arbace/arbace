# PY6d: array, a second program: slice assignment, subclass repr, sizeof, iterator reduce
import array, pickle, copy
a = array.array('i', [1,2,3,4,5])
a[1:3] = array.array('i', [9]); print(a, len(a))
del a[::2]; print(a); a.extend([7,8]); a += array.array('i',[1]); print(a, a*2, a.index(8), a.count(1))
b = array.array('d', [1.5, 2.5]); print(b, b.tobytes(), array.array('d', b.tobytes()))
print(a.__reduce_ex__(3)[0].__name__, pickle.loads(pickle.dumps(a, 2)), array.array('u','abc'))
class S(array.array): pass
s = S('b', [1,2]); print(repr(s), type(s[:1]), s == array.array('b',[1,2]), s < array.array('b',[1,3]))
it = iter(a); next(it); print(it.__reduce__()[1:], list(reversed(a)), memoryview(a).tobytes()[:4])
print(isinstance(a, __import__('collections.abc').abc.MutableSequence), copy.deepcopy(a), a.__sizeof__())
try: array.array('b', [300])
except OverflowError as e: print(e)

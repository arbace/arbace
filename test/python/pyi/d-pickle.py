# PY6d: pickle's Python Pickler and Unpickler (protocols 1-5) over the runtime's reduce protocol: built-in values,
# classes, exceptions (BaseException.__reduce__, OSError's filename, ImportError's name), built-in functions, arrays
import pickle, pickletools, io, collections, array
class C:
    def __init__(self): self.x = 1; self.y = [self]
class D(dict): pass
values = [None, True, 1, -2**70, 1.5, "sé\U0001f600", b"b\x00", bytearray(b"ba"), (1, (2,)), [1, [2]], {"a": {1: 2}},
          {1, 2}, frozenset({3}), complex(1, 2), range(3, 9, 2), slice(1, 2), collections.OrderedDict(a=1), D(k=2),
          array.array("i", [1, 2]), ValueError("bad", 3), OSError(2, "nope", "f.txt"), ImportError("m", name="n", path="p"),
          len, Ellipsis, NotImplemented, type, int]
for proto in range(1, 6):
    for v in values:
        try:
            s = pickle.dumps(v, proto)
            r = pickle.loads(s)
            print(proto, type(v).__name__, r == v if not isinstance(v, BaseException) else (type(r), r.args), len(s))
        except Exception as e:
            print(proto, type(v).__name__, "ERR", type(e).__name__, e)
    c = pickle.loads(pickle.dumps(C(), proto)); print(proto, c.x, c.y[0] is c)
print(pickle.dumps([1, "a", (2,)], 2), pickle.dumps({"k": b"v"}, 4))
e = pickle.loads(pickle.dumps(OSError(2, "nope", "f.txt", None, "g.txt"))); print(e.filename, e.filename2, e.errno)
i = pickle.loads(pickle.dumps(ImportError("m", name="n", path="p"))); print(i.name, i.path, i.msg)
x = ValueError(1); x.extra = 5; print(x.__reduce__(), pickle.loads(pickle.dumps(x)).extra)
out = io.StringIO(); pickletools.dis(pickle.dumps((1, "a"), 3), out); print(out.getvalue())
print(len.__reduce__(), [].append.__reduce__()[1][1])

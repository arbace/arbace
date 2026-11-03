# PY6d: array (every type code, slices with steps, comparisons, the iterator, pickling by machine format, messages)
import array, pickle, copy, sys
for tc in array.typecodes:
    if tc == "u": continue
    ex = {"f": [1.5, -2.25, 3.0], "d": [1.5, -2.25, 1e300], "w": "aé€"}.get(tc, [1, 2, 3, 120])
    a = array.array(tc, ex)
    b = a * 3
    b[1:7:2] = array.array(tc, b[0:3])
    del b[::4]
    b.extend(a); b.insert(-2, a[0]); b.reverse()
    print(tc, a.itemsize, len(b), b.tolist() if tc != "w" else b.tounicode(), a.tobytes().hex(), b.index(a[1]), b.count(a[0]))
    for proto in range(1, 6):
        c = pickle.loads(pickle.dumps(b, proto))
        assert c == b and type(c) is array.array, (tc, proto)
    it = iter(b); next(it)
    print(it.__reduce__()[2], pickle.loads(pickle.dumps(it)).__next__() == b[1], repr(array.array(tc)), copy.deepcopy(a) == a, a < b, a == a[:])
    s = a.tobytes(); r = array.array(tc); r.frombytes(s); r.byteswap(); r.byteswap(); print(r == a, r.buffer_info()[1])
x = array.array("b", [1, 2, 3])
for bad in (lambda: x.append(128), lambda: array.array("B", [-1]), lambda: array.array("H", [70000]), lambda: array.array("I", [-1]),
            lambda: array.array("Q", [2**64]), lambda: array.array("q", [2**63]), lambda: array.array("i", [1.5]), lambda: array.array("f", ["x"]),
            lambda: array.array("x"), lambda: array.array("bb"), lambda: array.array("i", "str"), lambda: x.pop(9), lambda: x.index(99),
            lambda: x[5], lambda: x + array.array("i"), lambda: x.frombytes(b"\0" * 3) if False else array.array("i").frombytes(b"\0" * 3),
            lambda: x.fromunicode("a"), lambda: x.__setitem__(slice(0, 3, 2), array.array("b", [1, 2, 3]))):
    try: bad()
    except Exception as e: print(type(e).__name__, e)
class S(array.array):
    pass
s = S("i", [5, 6]); s += array.array("i", [7]); print(repr(s), type(s[:1]).__name__, s * 2, 2 * s, s.__sizeof__() > 0)
print(array.array("d", [0.1]).__reduce_ex__(2)[0].__name__, array.array("d", [0.1]).__reduce_ex__(3)[1][1:3], list(reversed(array.array("h", [1, 2, 3]))))
print(array._array_reconstructor(array.array, "i", 9, b"\0\0\0\1\0\0\0\2"), array._array_reconstructor(array.array, "L", 12, b"\xff" * 8))

# PY6d: _struct's half floats (e), complex formats (F, D), Struct's __init__, pack_into/unpack_from offsets, iter_unpack
import struct, array, math
for x in (0.0, -0.0, 1.0, 65504.0, 6.1e-5, 5.96e-8, 2.98e-8, 1/3, -1e-10, float("inf"), -float("inf"), 0.5 + 2**-12, 1.0009765625):
    print(x, struct.pack("<e", x).hex(), struct.unpack(">e", struct.pack(">e", x))[0])
print(math.isnan(struct.unpack("e", struct.pack("e", float("nan")))[0]), struct.pack("<e", -float("nan")).hex())
for f in ("F", "D", "<F", ">D", "bF", "bD"):
    print(f, struct.calcsize(f), struct.unpack(f, struct.pack(f, *([1] if f[0] == "b" else []), 1.5 - 2j)))
s = struct.Struct(">h"); s.__init__(">hh"); print(s.format, s.size, s.pack(1, 2), repr(s))
u = struct.Struct.__new__(struct.Struct); print(u.size)
for bad in (lambda: u.pack(1), lambda: u.format, lambda: struct.pack("e", 65520.0), lambda: struct.pack("F", "x"),
            lambda: struct.pack_into("b", bytearray(1), 5, 1), lambda: struct.pack_into("<I", bytearray(10), -2, 1),
            lambda: struct.pack_into("<B", bytearray(10), -11, 1), lambda: struct.unpack_from("<I", bytearray(10), -2),
            lambda: struct.unpack_from("b", bytearray(1), 5), lambda: struct.calcsize("%db" % (2**63)), lambda: struct.calcsize("%d?s" % (2**63 - 1)),
            lambda: struct.pack_into("b", b"x", 0, 1), lambda: struct.iter_unpack("i", b"123"), lambda: struct.Struct(">").iter_unpack(b"")):
    try: bad()
    except Exception as e: print(type(e).__name__, e)
buf = array.array("b", b" " * 20); struct.pack_into("4s", memoryview(buf), 3, b"abcd"); print(buf.tobytes())
it = struct.iter_unpack(">IB", bytes(range(1, 16))); print(it.__length_hint__(), next(it), list(it), type(it).__name__)
print(struct.pack("3p", b"abc"), struct.pack("1000p", b"x" * 1000)[:3], struct.calcsize("1000000b"))

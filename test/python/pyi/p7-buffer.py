# PY7, PY10's synthetic divergences: memoryview's shape, strides, contiguity, count, index, toreadonly, a released
# view's attributes; bytes() of __bytes__; memoryview() and bytes() of a class's __buffer__ (PEP 688)
m = memoryview(b"abcdef")
print((m.c_contiguous, m.f_contiguous, m.contiguous), m.count(97), m.index(98), m.index(99, 1, 4))
print((m.format, m.itemsize, m.nbytes, m.ndim, m.shape, m.strides, m.suboffsets))
s = m[::2]
print(s.shape, s.strides, s.contiguous, s.tolist())
print(m.toreadonly().readonly, memoryview(bytearray(b"x")).readonly, memoryview(bytearray(b"x")).toreadonly().readonly)
try:
    m.index(120)
except ValueError as e:
    print("ValueError", e)
m.release()
for a in ("obj", "shape", "nbytes"):
    try:
        getattr(m, a)
    except ValueError as e:
        print(a, "ValueError", e)
print(hasattr(memoryview(b""), "released"))
class B:
    def __bytes__(self):
        return b"c"
class Bad:
    def __bytes__(self):
        return "c"
class SB(bytes):
    pass
print(bytes(B()), SB(B()), type(SB(B())).__name__)
try:
    bytes(Bad())
except TypeError as e:
    print("TypeError", e)
class C:
    def __buffer__(self, flags):
        print("buffer")
        return memoryview(b"xyz")
    def __release_buffer__(self, view):
        print("release")
v = memoryview(C())
print(bytes(v))
v.release()
v.release()
print(bytes(C()))
with memoryview(C()) as w:
    print(w.tobytes())

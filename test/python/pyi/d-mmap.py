# PYS22 (a), queue 204: mmap (Modules/mmapmodule.c's Linux paths ported to Go over syscall): a file's mapping read, sliced,
# assigned, searched, moved, flushed, resized and closed (its pages gone from /proc/self/maps), ACCESS_READ and COPY,
# anonymous shared and private mappings, the arguments' messages, a subclass, __index__ closing or resizing it
import mmap, os, re, tempfile
def show(f):
    try: print(repr(f()))
    except Exception as e: print(type(e).__name__, e)
d = tempfile.mkdtemp()
fn = os.path.join(d, 'f')
with open(fn, 'wb') as f:
    f.write(b'hello world\nline two\n' + b'\0' * (mmap.PAGESIZE - 21))
with open(fn, 'r+b') as f:
    m = mmap.mmap(f.fileno(), 0)
    print('mapped:', os.path.abspath(fn) in open('/proc/self/maps').read())
    print(len(m), m[:5], m[0], m[-1], m[2:9:3], m[::-1][:3], m.size())
    print(m.find(b'o'), m.rfind(b'o'), m.find(b'o', 5), m.find(b'o', 5, 7), m.find(b''), m.rfind(b'xyz'))
    print(m.readline(), m.tell(), m.read(4), m.read_byte(), m.seek(0), m.seek(-1, 2), m.seek(3, 0))
    m[0] = ord('H'); m[1:3] = b'EL'; m[4:10:2] = b'XYZ'
    m.write(b'!!'); m.write_byte(65)
    print(m[:12], m.tell())
    m.move(0, 6, 5); print(m[:12])
    print(m.flush(), m.flush(0, 10))
    show(lambda: m.flush(1, 6))
    show(lambda: m.flush(0, 10**6))
    print(re.search(rb'w.r', m) is not None, bytes(m[:3]), list(m[:3]), b'H' in m, [b for b in m][:2])
    mv = memoryview(m); mv[0] = 104; print(m[0], bytes(mv[:5]))
    del mv
    m.resize(2 * mmap.PAGESIZE); print(len(m), os.path.getsize(fn), m[:5])
    m.resize(100); print(len(m), os.path.getsize(fn))
    print(repr(m))
    print(m.madvise(mmap.MADV_NORMAL), m.madvise(mmap.MADV_WILLNEED, 0, 10))
    show(lambda: m.madvise(0, 200))
    with m:
        pass
    print(m.closed, repr(m))
    print('mapped after close:', os.path.abspath(fn) in open('/proc/self/maps').read())
    for f2 in [lambda: m[0], lambda: m.read(), len, lambda: m.find(b'a')]:
        show(lambda: f2(m) if f2 is len else f2())
    print(open(fn, 'rb').read(12))
with open(fn, 'rb') as f:
    r = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
    show(lambda: r.__setitem__(0, 1))
    show(lambda: r.write(b'x'))
    show(lambda: r.resize(10))
    show(lambda: memoryview(r).__setitem__(0, 1))
    c = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_COPY)
    c[0] = 0; print(c[0], r[0]); show(lambda: c.resize(10))
    r.close(); c.close()
a = mmap.mmap(-1, 16)
a.write(b'python'); print(a[:8], a.flush()); show(lambda: a.flush(1, 6)); show(lambda: a.size())
show(lambda: a.resize(32))
p = mmap.mmap(-1, 16, flags=mmap.MAP_PRIVATE)
p[:3] = b'abc'; p.resize(9000); print(len(p), p[:4]); p.resize(5); print(len(p), p[:])
for f in [lambda: mmap.mmap(-1, 0), lambda: mmap.mmap(-1, -1), lambda: mmap.mmap(-1, 10, access=7),
          lambda: mmap.mmap(-1, 10, prot=1, access=1), lambda: mmap.mmap(), lambda: mmap.mmap(-1, 10, foo=1),
          lambda: mmap.mmap(2**40, 10), lambda: mmap.mmap(-1, 10, offset=-1), lambda: a[2**70], lambda: a['x'],
          lambda: a.__setitem__(0, 256), lambda: a.__setitem__(0, 1.0), lambda: a.__delitem__(0), lambda: a.__setitem__(slice(0, 2), b'abc'),
          lambda: a.write_byte(256), lambda: a.read('x'), lambda: a.seek(0, 3), lambda: a.seek(17), lambda: a.move(0, 10, 10),
          lambda: a.close(1), lambda: a.resize(), lambda: a.find('x'), lambda: a.madvise(999)]:
    show(f)
class M(mmap.mmap):
    pass
s = M(-1, 8); s[0] = 7; print(type(s).__name__, s[0], len(s), repr(s))
import weakref; print(weakref.ref(a)() is a)
class X:
    def __index__(self):
        a.close(); return 1
show(lambda: a[X()])
b = mmap.mmap(-1, 2 * mmap.PAGESIZE)
class R:
    def __index__(self):
        b.resize(mmap.PAGESIZE); return 0
show(lambda: b.__setitem__(2 * mmap.PAGESIZE - 1, R()))
print(len(b))
print(sorted(k for k in vars(mmap) if k.isupper()))
# a memoryview of an mmap: written through, sliced, released, then the mmap closed (CPython refuses the close while a view
# is exported; pyi counts no exports, so a view used after the close raises ValueError "mmap closed or invalid": not
# comparable here, the queue 204 report's probe)
w = mmap.mmap(-1, 16)
v = memoryview(w); v[1] = 66; v[2:4] = b'CD'; print(w[:5], bytes(v[1:4]), len(v), v[3])
v.release(); w.close(); print(w.closed)
show(lambda: v[0])
import shutil; shutil.rmtree(d)

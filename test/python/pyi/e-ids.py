# PY6e2: the set*id family, getres[ug]id, groups, putenv's checks, the scheduler's bounds, eventfd, preadv, pwritev, fallocate, fadvise, lockf, stat_result's pickling
import os, sys, pickle
import atexit, itertools as _it, shutil
_n = _it.count()
def mkdtemp():
    d = os.path.join(os.environ.get("TMPDIR", "."), "e-%s-%d-%d" % (__file__.split("/")[-1][2:-3], os.getpid(), next(_n)))
    os.mkdir(d)
    atexit.register(shutil.rmtree, d, True)
    return d
for x in [("getcwd", 1), ("pipe2", 0, 0)]:
    try: getattr(os, x[0])(*x[1:]); print("no error", x)
    except TypeError as e: print("TypeError", x[0])
for x in [-2, 2**64, -2**63-1]:
    try: os.major(x); print("no error")
    except (ValueError, OverflowError) as e: print(type(e).__name__)
try: os.major(1.0)
except TypeError: print("major float TypeError")
for f in (os.setuid, os.setgid, os.seteuid, os.setegid):
    try: f(1 << 32)
    except OverflowError as e: print(e)
os.setreuid(-1, -1); os.setregid(-1, -1)
print(os.getresuid() == (os.getuid(), os.geteuid(), os.getuid()), len(os.getresgid()))
print(os.getgrouplist("root", 0)[:1])
for k, v in [('FRUIT\0V', 'c'), ('F', 'o\0V'), ('F=O', 'l'), (b'F=O', b'l')]:
    try: os.putenv(k, v); print("no error")
    except ValueError as e: print("ValueError", e)
print(os.sched_get_priority_min(os.SCHED_FIFO), os.sched_get_priority_max(os.SCHED_FIFO))
try: os.sched_getscheduler(0)
except OSError as e: print(e.errno)
fd = os.eventfd(3); os.eventfd_write(fd, 4); print(os.eventfd_read(fd)); os.close(fd)
r, w = os.pipe()
with open(os.path.join(mkdtemp(), "f"), "w+b") as f:
    n = os.pwritev(f.fileno(), [b"hello", b"world"], 0)
    a, b = bytearray(3), bytearray(10)
    print(n, os.preadv(f.fileno(), [a, b], 1), a, b)
    os.posix_fallocate(f.fileno(), 0, 100); print(os.fstat(f.fileno()).st_size)
    os.posix_fadvise(f.fileno(), 0, 0, os.POSIX_FADV_NORMAL)
    os.lockf(f.fileno(), os.F_LOCK, 0); os.lockf(f.fileno(), os.F_TEST, 0); os.lockf(f.fileno(), os.F_ULOCK, 0)
try: os.posix_fallocate(-1, 0, 1)
except OSError as e: print(e.errno)
print(os.reload_environ is not None, os.ctermid())
st = os.stat(".")
print(type(st).__reduce__ is not object.__reduce__)
print(st.__reduce__()[0])
try: print(pickle.loads(pickle.dumps(st)) == st)
except Exception as e: print("ERR", e)
try: print(st.__reduce_ex__(2)[0])
except Exception as e: print("ERR", e)

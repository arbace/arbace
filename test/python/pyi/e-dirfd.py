# PY6e2: dir_fd and the *at functions, os.supports_*, fwalk, listdir and scandir of a descriptor, path_converter's checks
import os, stat
import atexit, itertools as _it, shutil
_n = _it.count()
def mkdtemp():
    d = os.path.join(os.environ.get("TMPDIR", "."), "e-%s-%d-%d" % (__file__.split("/")[-1][2:-3], os.getpid(), next(_n)))
    os.mkdir(d)
    atexit.register(shutil.rmtree, d, True)
    return d
print(sorted(f.__name__ for f in os.supports_dir_fd))
print(sorted(f.__name__ for f in os.supports_fd))
print(sorted(f.__name__ for f in os.supports_follow_symlinks))
d = mkdtemp()
fd = os.open(d, os.O_RDONLY)
f = os.open("a", os.O_WRONLY | os.O_CREAT, 0o644, dir_fd=fd); os.write(f, b"x"); os.close(f)
print(os.stat("a", dir_fd=fd).st_size, os.listdir(fd))
os.mkdir("sub", dir_fd=fd); os.symlink("a", "l", dir_fd=fd); print(os.readlink("l", dir_fd=fd))
os.rename("a", "b", src_dir_fd=fd, dst_dir_fd=fd); print(sorted(os.listdir(fd)))
try: os.link("l", "hl0", src_dir_fd=fd, dst_dir_fd=fd)
except OSError as e: print(e)
os.symlink("b", "l2", dir_fd=fd); os.link("l2", "hl", src_dir_fd=fd, dst_dir_fd=fd); print(stat.S_ISLNK(os.lstat(os.path.join(d, "hl")).st_mode))
os.utime("l", ns=(5*10**9, 7*10**9), dir_fd=fd, follow_symlinks=False); print(os.lstat("l", dir_fd=fd).st_mtime)
print(os.access("b", os.R_OK, dir_fd=fd))
for root, dirs, files, rootfd in os.fwalk(d):
    print(root == d, sorted(dirs), sorted(files), type(rootfd))
with os.scandir(fd) as it:
    print(sorted((e.name, e.path, e.is_dir()) for e in it))
os.unlink("hl", dir_fd=fd); os.rmdir("sub", dir_fd=fd)
try: os.stat("zz", dir_fd=99)
except OSError as e: print(type(e).__name__, e.errno)
try: os.stat(0, follow_symlinks=False)
except ValueError as e: print(e)
try: os.stat(1.5)
except TypeError as e: print(e)
try: os.stat("a\0b")
except ValueError as e: print(e)
class P:
    def __fspath__(self): return 3
try: os.stat(P())
except TypeError as e: print(e)
import shutil; shutil.rmtree(d); os.close(fd)

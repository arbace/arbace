# PY6e: the OS modules' argument checks and edge cases Lib/test's test_poll, test_epoll, test_fcntl, test_resource,
# test_pwd, test_grp and test_stat ask; equal to CPython
import os, select, fcntl, resource, pwd, grp, socket, stat, _stat
def err(f, *a, **k):
    try:
        r = f(*a, **k); return 'ok %r' % (r,)
    except Exception as e:
        return type(e).__name__
class Bad:
    def __init__(self, v): self.v = v
    def fileno(self): return self.v
print(err(fcntl.fcntl, Bad(-1), fcntl.F_SETFL, 0), err(fcntl.fcntl, Bad('x'), fcntl.F_SETFL, 0))
r, w = os.pipe(); os.close(r); os.close(w)
print(err(fcntl.fcntl, r, fcntl.F_DUPFD, b'\0' * 64))
p = select.poll(); p.register(r); print(p.poll() == [(r, select.POLLNVAL)])
print(err(p.register, 0, -1), err(p.register, 0, 1 << 64), err(p.poll, 1 << 64))
print(err(select.epoll, flags=12356), err(select.epoll, 0), err(select.epoll, None))
print(err(resource.getrlimit), err(resource.getrlimit, 2**1000), err(resource.setrlimit, 0, (42,)), err(resource.setrlimit, 0, 42))
cur, mx = resource.getrlimit(resource.RLIMIT_FSIZE)
print(err(resource.setrlimit, resource.RLIMIT_FSIZE, (-5, mx)), err(resource.getrusage), type(resource.getrusage(0)).__name__)
print(err(pwd.getpwuid), err(pwd.getpwuid, 3.14), err(pwd.getpwuid, 2**128), err(pwd.getpwnam, 'a\x00b'), err(pwd.getpwnam, b'root'), err(pwd.getpwall, 1))
print(err(grp.getgrgid, 2**128), err(grp.getgrnam, 'roo\udc74'), grp.getgrnam('root').gr_gid)
print(_stat.filemode(0o140755), _stat.filemode(0o104755), _stat.S_ISDIR(0o040000), err(_stat.S_ISDIR, -1), err(_stat.filemode, 2**40), stat.filemode is _stat.filemode)
d = os.path.join(os.environ.get('TMPDIR', '.'), 'e-oschecks-%d' % os.getpid()); os.mkdir(d); old = os.getcwd(); os.chdir(d)
s = socket.socket(socket.AF_UNIX); s.bind('@sock'); print(s.getsockname(), stat.S_ISSOCK(os.stat('@sock').st_mode)); s.close()
os.unlink('@sock'); os.chdir(old); os.rmdir(d)
print([err(f, 0) for f in (os.read, os.readinto, os.write, os.lseek, os.kill, os.pread)], err(os.close), err(os.umask))

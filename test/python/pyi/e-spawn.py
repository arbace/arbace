# PY6e2: posix_spawn, posix_spawnp and the spawn* functions over ForkExec, their file actions and checks
import os, sys
import atexit, itertools as _it, shutil
_n = _it.count()
def mkdtemp():
    d = os.path.join(os.environ.get("TMPDIR", "."), "e-%s-%d-%d" % (__file__.split("/")[-1][2:-3], os.getpid(), next(_n)))
    os.mkdir(d)
    atexit.register(shutil.rmtree, d, True)
    return d
# spawn
pid = os.posix_spawn("/bin/sh", ["sh", "-c", "exit 3"], os.environ)
print(os.waitstatus_to_exitcode(os.waitpid(pid, 0)[1]))
pid = os.posix_spawnp("sh", ["sh", "-c", "exit 4"], {"PATH": "/bin"})
print(os.waitstatus_to_exitcode(os.waitpid(pid, 0)[1]))
t = os.path.join(mkdtemp(), "out")
pid = os.posix_spawn("/bin/sh", ["sh", "-c", "echo opened; ls /proc/self/fd/0 2>&1 >/dev/null | head -1 | cut -c1-2"], os.environ,
                     file_actions=[(os.POSIX_SPAWN_OPEN, 1, t, os.O_WRONLY | os.O_CREAT, 0o600), (os.POSIX_SPAWN_CLOSE, 0), (os.POSIX_SPAWN_DUP2, 1, 2)])
os.waitpid(pid, 0); print(open(t).read().split())
try: os.posix_spawn("no_such_executable", ["x"], os.environ)
except FileNotFoundError as e: print(e.filename)
for fa in ([None], [()], [(None,)], [(12345,)], [(1,)], [(1, 1, 2)], [(1, None)]):
    try: os.posix_spawn("/bin/true", ["true"], os.environ, file_actions=fa); print("no error", fa)
    except TypeError as e: print("TypeError", e)
for kw in [dict(setpgroup="023"), dict(setsigmask=34), dict(setsigmask=["j"]), dict(scheduler=1)]:
    try: os.posix_spawn("/bin/true", ["true"], os.environ, **kw); print("no error")
    except TypeError as e: print("TypeError", e)
try: os.posix_spawn("/bin/true", ["true"], os.environ, setsigdef=[65, 66])
except ValueError as e: print(e)
pid = os.posix_spawn("/bin/true", ["true"], os.environ, setsid=True, resetids=True); print(os.waitpid(pid, 0)[1])
pid = os.posix_spawn("/bin/true", ["true"], os.environ, setpgroup=0); print(os.waitpid(pid, 0)[1])
pid = os.posix_spawn("/bin/true", ["true"], os.environ, file_actions=[(os.POSIX_SPAWN_OPEN, 1, "/dev/null", os.O_WRONLY, 0)]); print(os.waitpid(pid, 0)[1])
print(os.spawnv(os.P_WAIT, "/bin/sh", ["sh", "-c", "exit 5"]), os.spawnlp(os.P_WAIT, "sh", "sh", "-c", "exit 6"),
      os.spawnve(os.P_WAIT, "/nonexistent", ["x"], {}), os.spawnle(os.P_WAIT, "/bin/sh", "sh", "-c", "exit $E", {"E": "7"}))
pid = os.spawnv(os.P_NOWAIT, "/bin/sh", ["sh", "-c", "exit 8"]); print(os.waitstatus_to_exitcode(os.waitpid(pid, 0)[1]))
for a in [(), [], ("",)]:
    try: os.spawnv(os.P_NOWAIT, "/bin/true", a)
    except ValueError as e: print("ValueError")

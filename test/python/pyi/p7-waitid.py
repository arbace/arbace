# PY7: os.waitid with WNOWAIT (asyncio's _ThreadedChildWatcher waits so; it was EINVAL and no asyncio subprocess
# ended): the child's status left for waitpid, WNOHANG before it exits, ChildProcessError once it is reaped
import asyncio, os, subprocess

def show(r):
    return None if r is None else (r.si_pid == pid, r.si_signo, r.si_status, r.si_code)

p = subprocess.Popen(["/bin/sh", "-c", "read x; exit 3"], stdin=subprocess.PIPE)
pid = p.pid
print("running", show(os.waitid(os.P_PID, pid, os.WEXITED | os.WNOWAIT | os.WNOHANG)))
p.stdin.close()
print("exited", show(os.waitid(os.P_PID, pid, os.WEXITED | os.WNOWAIT)))
print("again", show(os.waitid(os.P_PID, pid, os.WEXITED | os.WNOWAIT | os.WNOHANG)))
print("reaped", os.waitpid(pid, 0) == (pid, 3 << 8))
try:
    os.waitid(os.P_PID, pid, os.WEXITED | os.WNOWAIT)
except ChildProcessError as e:
    print("ChildProcessError", e.errno)
q = subprocess.Popen(["/bin/sh", "-c", "kill -9 $$"])
pid = q.pid
print("killed", show(os.waitid(os.P_PID, pid, os.WEXITED | os.WNOWAIT)), q.wait())
try:
    os.waitid(os.P_PID, os.getpid(), os.WEXITED | os.WNOWAIT | os.WNOHANG)
except ChildProcessError as e:
    print("not a child", e.errno)

async def main():
    proc = await asyncio.create_subprocess_exec("/bin/sh", "-c", "exit 5")
    return await asyncio.wait_for(proc.wait(), 60)
print("asyncio", asyncio.run(main()))

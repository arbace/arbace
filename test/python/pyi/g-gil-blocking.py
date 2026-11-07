# A thread blocked in a system call releases the GIL (Py_BEGIN_ALLOW_THREADS around recv, accept, connect, os.read,
# select, poll, epoll, waitpid): the thread that unblocks it runs meanwhile (queue 200, PY6-core, integrating PY6e's
# OS family under PY6f's GIL); without it each case deadlocks
import os, select, socket, subprocess, sys, threading, time

def later(f):
    # f after a pause, so the main thread is already blocked when f is needed
    t = threading.Thread(target=lambda: (time.sleep(0.5), f()))
    t.start()
    return t

a, b = socket.socketpair()
t = later(lambda: b.sendall(b"ping"))
print("recv", a.recv(4))
t.join()

r, w = os.pipe()
t = later(lambda: os.write(w, b"pipe"))
print("os.read", os.read(r, 4))
t.join()

t = later(lambda: os.write(w, b"x"))
print("select", select.select([r], [], [], 30)[0] == [r], os.read(r, 1))
t.join()

p = select.poll(); p.register(r, select.POLLIN)
t = later(lambda: os.write(w, b"y"))
print("poll", len(p.poll(30000)), os.read(r, 1))
t.join()

e = select.epoll(); e.register(r, select.EPOLLIN)
t = later(lambda: os.write(w, b"z"))
print("epoll", len(e.poll(30)), os.read(r, 1))
t.join()

srv = socket.socket(); srv.bind(("127.0.0.1", 0)); srv.listen()
t = later(lambda: socket.create_connection(srv.getsockname()).sendall(b"conn"))
c, _ = srv.accept()
print("accept", c.recv(4))
t.join()

proc = subprocess.Popen([sys.executable, "-c", "import sys; sys.stdin.read(1)"], stdin=subprocess.PIPE)
t = later(lambda: (proc.stdin.write(b"q"), proc.stdin.close()))
print("waitpid", os.waitpid(proc.pid, 0)[1])
t.join()

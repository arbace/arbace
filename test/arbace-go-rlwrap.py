#!/usr/bin/env python3
# The Go executable's REPL under rlwrap (doc/go/EXEC-NOTES.md, "Line editing: rlwrap"), checked
# under a pseudo-terminal: bin/arbace-go --smoke runs it on amd64 when python3 and rlwrap are
# installed. Usage: arbace-go-rlwrap.py EXECUTABLE TMPDIR (rlwrap's HOME, for its history file,
# is made in TMPDIR). Prints one line per check, exit status 1 when one fails.
import os, pty, select, shutil, signal, struct, sys, tempfile, termios, fcntl, time

exe = os.path.abspath(sys.argv[1])
base_env = {k: v for k, v in os.environ.items() if not k.startswith("ARBACE_RLWRAP")}
base_env["TERM"] = "xterm"
# rlwrap keeps its history in ~/.arbace_history: a scratch home
home = tempfile.mkdtemp(prefix="rlwrap-home.", dir=sys.argv[2])
base_env["HOME"] = home
fails = 0


def comm(pid):
    try:
        with open(f"/proc/{pid}/comm") as f:
            return f.read().strip()
    except OSError:
        return None


def children(pid):
    try:
        with open(f"/proc/{pid}/task/{pid}/children") as f:
            return [int(c) for c in f.read().split()]
    except OSError:
        return []


class Session:
    """The executable on a pty (24x80, TERM=xterm unless env says otherwise)."""

    def __init__(self, args, env=None):
        e = dict(base_env)
        e.update(env or {})
        self.pid, self.fd = pty.fork()
        if self.pid == 0:
            fcntl.ioctl(0, termios.TIOCSWINSZ, struct.pack("HHHH", 24, 80, 0, 0))
            os.execve(exe, [exe] + args, e)
        self.out = b""

    def read_until(self, pattern, timeout=20.0, count=1):
        end = time.time() + timeout
        while self.out.count(pattern) < count and time.time() < end:
            r, _, _ = select.select([self.fd], [], [], 0.1)
            if r:
                try:
                    data = os.read(self.fd, 4096)
                except OSError:
                    break
                if not data:
                    break
                self.out += data
        return self.out.count(pattern) >= count

    def send(self, data):
        os.write(self.fd, data)

    def finish(self, timeout=20.0):
        end = time.time() + timeout
        while time.time() < end:
            pid, st = os.waitpid(self.pid, os.WNOHANG)
            if pid:
                # drain
                self.read_until(b"\0", timeout=0.2)
                return os.waitstatus_to_exitcode(st)
            self.read_until(b"\0", timeout=0.1)
        os.kill(self.pid, signal.SIGKILL)
        os.waitpid(self.pid, 0)
        return None


def check(name, ok, detail=""):
    global fails
    print(("  ok   " if ok else "  FAIL ") + name)
    if not ok:
        fails += 1
        if detail:
            print("    " + detail)


def repl(name, env, expect_rlwrap, args=()):
    s = Session(list(args), env)
    prompted = s.read_until(b"user=> ")
    # the process forked onto the pty is rlwrap itself (exec, no lingering parent), and its
    # child the executable
    top = comm(s.pid)
    kids = [comm(c) for c in children(s.pid)]
    s.send(b'(System/getenv "ARBACE_RLWRAP")\r')
    s.read_until(b'"wrapped"\r' if expect_rlwrap else b"user=> ", count=1 if expect_rlwrap else 2)
    time.sleep(0.2)
    s.send(b"(+ 40 2)\r")
    got42 = s.read_until(b"42\r", count=1)
    if expect_rlwrap:
        time.sleep(0.3)
        s.send(b"\x1b[A")  # the up arrow: rlwrap recalls (+ 40 2)
        time.sleep(0.3)
        s.send(b"\r")
        recalled = s.read_until(b"42\r", count=2)
    time.sleep(0.2)
    s.send(b"\x04")  # end of input
    status = s.finish()
    text = s.out.decode("utf-8", "replace")
    if expect_rlwrap:
        check(f"{name}: the REPL runs under rlwrap (the pty's process is rlwrap, its child arbace)",
              prompted and top == "rlwrap" and kids == ["arbace"], f"process {top}, children {kids}")
        check(f"{name}: ARBACE_RLWRAP=wrapped in the child", '"wrapped"' in text, repr(text[-400:]))
        check(f"{name}: the up arrow recalls the last input", got42 and recalled, repr(text[-400:]))
    else:
        check(f"{name}: the REPL runs without rlwrap", prompted and top == "arbace" and kids == [],
              f"process {top}, children {kids}")
        seen = (env or {}).get("ARBACE_RLWRAP")
        shown = f'"{seen}"' if seen else "nil"
        check(f"{name}: ARBACE_RLWRAP in the REPL is {shown}, not set by the executable",
              got42 and ("\n" + shown + "\n") in text.replace("\r", ""), repr(text[-400:]))
    check(f"{name}: exit status 0 at end of input", status == 0, f"status {status}")
    check(f"{name}: nothing about rlwrap printed", "rlwrap" not in text, repr(text[-400:]))


def one_shot(name, args, expected, env=None):
    s = Session(list(args), env)
    status = s.finish()
    text = s.out.decode("utf-8", "replace").replace("\r", "")
    check(f"{name}: no rlwrap", status == 0 and text == expected, f"status {status}, output {text!r}")


repl("the REPL on a terminal", {}, True)
repl("-e then -r", {}, True, ["-e", "(def a 1)", "-r"])
repl("ARBACE_RLWRAP=off", {"ARBACE_RLWRAP": "off"}, False)
repl("TERM=dumb", {"TERM": "dumb"}, False)
repl("rlwrap not on PATH", {"PATH": "/nonexistent"}, False)
one_shot("-e on a terminal", ["-e", '(prn (System/getenv "ARBACE_RLWRAP"))'], "nil\n")
one_shot("-e alone (init options only)", ["-e", "(println 1)"], "1\n")
shutil.rmtree(home, ignore_errors=True)
sys.exit(1 if fails else 0)

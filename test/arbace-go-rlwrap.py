#!/usr/bin/env python3
# The Go executable's REPL under rlwrap (doc/go/EXEC-NOTES.md, "Line editing: rlwrap"), checked
# under a pseudo-terminal: bin/arbace-go --smoke runs it on amd64 when python3 and rlwrap are
# installed; and the box's launcher's (bin/arbace-go --smoke --box; doc/go/BOX-NOTES.md, S1),
# which follows the same convention. Usage: arbace-go-rlwrap.py EXECUTABLE TMPDIR (rlwrap's
# HOME, for its history file, is made in TMPDIR). Prints one line per check, exit status 1 when
# one fails.
import os, pty, select, shutil, signal, struct, sys, tempfile, termios, fcntl, time

exe = os.path.abspath(sys.argv[1])
# the executable's process name (/proc/PID/comm: its file name, at most 15 bytes)
name = os.path.basename(exe)[:15]
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
            # as from a login shell: a script's background job (bin/gate's) starts with SIGINT
            # and SIGQUIT ignored, and an ignored signal stays ignored across exec
            signal.signal(signal.SIGINT, signal.SIG_DFL)
            signal.signal(signal.SIGQUIT, signal.SIG_DFL)
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


def repl(label, env, expect_rlwrap, args=()):
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
        check(f"{label}: the REPL runs under rlwrap (the pty's process is rlwrap, its child {name})",
              prompted and top == "rlwrap" and kids == [name], f"process {top}, children {kids}")
        check(f"{label}: ARBACE_RLWRAP=wrapped in the child", '"wrapped"' in text, repr(text[-400:]))
        check(f"{label}: the up arrow recalls the last input", got42 and recalled, repr(text[-400:]))
    else:
        check(f"{label}: the REPL runs without rlwrap", prompted and top == name and kids == [],
              f"process {top}, children {kids}")
        seen = (env or {}).get("ARBACE_RLWRAP")
        shown = f'"{seen}"' if seen else "nil"
        check(f"{label}: ARBACE_RLWRAP in the REPL is {shown}, not set by the executable",
              got42 and ("\n" + shown + "\n") in text.replace("\r", ""), repr(text[-400:]))
    check(f"{label}: exit status 0 at end of input", status == 0, f"status {status}")
    check(f"{label}: nothing about rlwrap printed", "rlwrap" not in text, repr(text[-400:]))


def ctrl_c(label, env):
    """Ctrl-C at the prompt ends the REPL (the terminal's SIGINT), as it ends the JVM's."""
    s = Session([], env)
    prompted = s.read_until(b"user=> ")
    time.sleep(0.2)
    s.send(b"\x03")
    status = s.finish(timeout=10.0)
    check(f"{label}: Ctrl-C ends it", prompted and status is not None, f"status {status}")


def one_shot(label, args, expected, env=None):
    s = Session(list(args), env)
    status = s.finish()
    text = s.out.decode("utf-8", "replace").replace("\r", "")
    check(f"{label}: no rlwrap", status == 0 and text == expected, f"status {status}, output {text!r}")


repl("the REPL on a terminal", {}, True)
repl("-e then -r", {}, True, ["-e", "(def a 1)", "-r"])
repl("ARBACE_RLWRAP=off", {"ARBACE_RLWRAP": "off"}, False)
repl("TERM=dumb", {"TERM": "dumb"}, False)
repl("rlwrap not on PATH", {"PATH": "/nonexistent"}, False)
ctrl_c("Ctrl-C under rlwrap", {})
ctrl_c("Ctrl-C without rlwrap", {"ARBACE_RLWRAP": "off"})
one_shot("-e on a terminal", ["-e", '(prn (System/getenv "ARBACE_RLWRAP"))'], "nil\n")
one_shot("-e alone (init options only)", ["-e", "(println 1)"], "1\n")
shutil.rmtree(home, ignore_errors=True)
sys.exit(1 if fails else 0)

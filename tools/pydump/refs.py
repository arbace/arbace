#!/usr/bin/env python3
"""The Python crop's reference runs (doc/python/PYTHON-SURVEY.md §8.1, PYS14 (a), PYS15 (a); GTS207 (a)): CPython 3.14.10
runs the programs and Lib/test's modules, and the outcomes are written as data for bin/pyi record-ref.

usage: python3 -I -S -B refs.py VERB ...
  programs --base DIR --list FILE --work DIR --out FILE [--timeout S]
      each program of the list (paths relative to DIR) run by the oracle: python3 -s -P -B PROGRAM in a fresh directory
      of WORK, an empty standard input, no arguments, under the fixed environment (env), killed after S seconds (20).
      One line a program: ID STATUS STDOUT-SHA256 BYTES FIRST-LINE STDERR-LAST-LINE SECONDS (tab-separated; STATUS the
      exit status, 128+N for a signal, "timeout")
  libtest --pypath DIR --modules M,M,... --work DIR --out FILE [--timeout S]
      each module run by regrtest (python3 -s -P -B -m test -u none --randseed 1 --timeout S --junit-xml), its
      resources all disabled (no network, no CPU-heavy or GUI tests), PYTHONPATH the directory holding the tarball's
      Lib/test as test (the host's own stdlib otherwise); per test method one line: MODULE ID OUTCOME REASON SECONDS
      (OUTCOME ok, skipped, failure, error; REASON a skip's message), and per module one line: MODULE - module STATUS
      SECONDS (regrtest's exit status)

The fixed environment (env): only PATH, HOME and TMPDIR (both inside WORK), PYTHONHASHSEED=0, LC_ALL=C.UTF-8, TZ=UTC,
PYTHONIOENCODING=utf-8, PYTHONDONTWRITEBYTECODE=1. The oracle runs with -s -P -B, not -I: -I implies -E, which would
ignore PYTHONHASHSEED (the survey's §8.1 lists both; the seed wins, -P keeps the script's directory off sys.path).
"""
import hashlib
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


def env(work):
    home = os.path.join(work, "home")
    tmp = os.path.join(work, "tmp")
    os.makedirs(home, exist_ok=True)
    os.makedirs(tmp, exist_ok=True)
    return {"PATH": "/usr/bin:/bin", "HOME": home, "TMPDIR": tmp, "PYTHONHASHSEED": "0", "LC_ALL": "C.UTF-8",
            "TZ": "UTC", "PYTHONIOENCODING": "utf-8", "PYTHONDONTWRITEBYTECODE": "1"}


# what a run's output must not hold for its reference to be the main host's too (amd64): addresses, the machine's
# architecture, its C library, its sizes; a skip's reason naming them
MACHINE = re.compile(r"0x[0-9a-f]{8,}|\bat 0x|(?<!\\)\b(?:aarch64|arm64|armv\d|x86_64|x86|amd64|i[3-6]86|musl|glibc|ppc64|s390x|"
                     r"little-endian|big-endian|sizeof|\d\d-bit)\b", re.I)


# a failure of the runner, not of CPython: regrtest's subprocesses (script_helper) run python3 -I, which drops
# PYTHONPATH, so they cannot import the tarball's Lib/test (the host's stdlib has no test package: Alpine's
# python3-tests, PYS1 (a), not installed)
RUNNER = re.compile(r"No module named \\*'test\\*'")


def clean(s):
    return s.replace("\t", " ").replace("\r", " ").replace("\n", " ")


def arg(args, name, default=None):
    return args[args.index(name) + 1] if name in args else default


def programs(args):
    base, lst, work, out = arg(args, "--base"), arg(args, "--list"), arg(args, "--work"), arg(args, "--out")
    timeout = float(arg(args, "--timeout", "20"))
    e = env(work)
    with open(out, "w", encoding="utf-8") as o:
        for i, rel in enumerate(l for l in open(lst, encoding="utf-8").read().split("\n") if l):
            cwd = os.path.join(work, "run", str(i))
            os.makedirs(cwd, exist_ok=True)
            t0 = time.perf_counter()
            try:
                p = subprocess.run(["python3", "-s", "-P", "-B", os.path.join(os.path.abspath(base), rel)], cwd=cwd,
                                   env=e, stdin=subprocess.DEVNULL, capture_output=True, timeout=timeout)
                status = str(p.returncode if p.returncode >= 0 else 128 - p.returncode)
                so, se = p.stdout, p.stderr
            except subprocess.TimeoutExpired as x:
                status, so, se = "timeout", x.stdout or b"", x.stderr or b""
            secs = time.perf_counter() - t0
            text = so.decode("utf-8", "replace")
            first = text.split("\n", 1)[0][:80]
            err = [l for l in se.decode("utf-8", "replace").split("\n") if l.strip()]
            flags = ",".join(sorted({m.group(0).lower() for m in MACHINE.finditer(text + "\n" + se.decode("utf-8", "replace"))}))
            o.write("%s\t%s\t%s\t%d\t%s\t%s\t%.3f\t%s\n" % (rel, status, hashlib.sha256(so).hexdigest(), len(so), clean(first),
                                                           clean(err[-1][:160]) if err else "", secs, flags[:120] or "-"))


def libtest(args):
    pypath, mods, work, out = arg(args, "--pypath"), arg(args, "--modules").split(","), arg(args, "--work"), arg(args, "--out")
    timeout = arg(args, "--timeout", "900")
    e = dict(env(work), PYTHONPATH=os.path.abspath(pypath))
    with open(out, "w", encoding="utf-8") as o:
        for m in mods:
            cwd = os.path.join(work, "lt", m)
            os.makedirs(cwd, exist_ok=True)
            xml = os.path.join(cwd, "junit.xml")
            t0 = time.perf_counter()
            try:
                p = subprocess.run(["python3", "-s", "-P", "-B", "-m", "test", "-u", "none", "--randseed", "1", "--timeout",
                                    timeout, "--junit-xml", xml, m], cwd=cwd, env=e, stdin=subprocess.DEVNULL,
                                   capture_output=True, timeout=float(timeout) + 60)
                status = str(p.returncode)
                open(os.path.join(cwd, "output.txt"), "wb").write(p.stdout + p.stderr)
            except subprocess.TimeoutExpired:
                status = "timeout"
            secs = time.perf_counter() - t0
            if os.path.exists(xml):
                for tc in ET.parse(xml).getroot().iter("testcase"):
                    outcome, reason = "ok", ""
                    for child in tc:
                        if child.tag in ("skipped", "failure", "error"):
                            outcome = child.tag
                            reason = child.get("message", "") or (child.text or "")
                    flags = ",".join(sorted({x.group(0).lower() for x in MACHINE.finditer(reason)}))
                    if outcome in ("failure", "error") and RUNNER.search(reason):
                        flags = "runner"
                    o.write("%s\t%s\t%s\t%s\t%s\t%s\n" % (m, tc.get("name"), outcome, clean(reason)[:200], tc.get("time", "0"),
                                                         flags or "-"))
            o.write("%s\t-\tmodule\t%s\t%.3f\t-\n" % (m, status, secs))


def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def rows(path):
    return [l.split("\t") for l in open(path, encoding="utf-8").read().split("\n") if l]


def entries(kind, path):
    """{id: (the reference's fields as EDN text, flags, seconds)} of a run's TSV"""
    out = {}
    for r in rows(path):
        if kind == "programs":
            rid, status, sha, n, first, err, secs, flags = r
            st = ":timeout" if status == "timeout" else status
            fields = ":status %s :stdout %s :bytes %s :first %s" % (st, q(sha), n, q(first))
            if err:
                fields += " :stderr-last %s" % q(err)
            out[rid] = (fields, flags, secs)
        else:
            mod, tid, outcome, reason, secs, flags = r
            if tid == "-":
                out[mod] = (":module true :status %s" % (":timeout" if reason == "timeout" else reason), "-", secs)
            else:
                f = ":outcome :%s" % outcome
                if reason:
                    f += " :reason %s" % q(reason)
                out[mod + "/" + tid] = (f, flags, secs)
    return out


def record(args):
    """A reference file from two runs (A, B): an entry the two runs agree on and whose output names nothing of the
    machine is a reference; the others are written :excluded with the reason (bin/pyi record-ref --check skips them)"""
    kind, a, b, out, costs = arg(args, "--kind"), arg(args, "--a"), arg(args, "--b"), arg(args, "--out"), arg(args, "--costs")
    header = open(arg(args, "--header"), encoding="utf-8").read()
    ea, eb = entries(kind, a), entries(kind, b)
    lines, cost, excluded = [], [], 0
    for rid in sorted(set(ea) | set(eb)):
        x, y = ea.get(rid), eb.get(rid)
        if x is None or y is None or x[0] != y[0]:
            lines.append("{:id %s :excluded \"unstable: the two recording runs differ\"}" % q(rid))
            excluded += 1
        elif x[1] == "runner":
            lines.append("{:id %s :excluded %s}" % (q(rid), q("runner: a subprocess run -I cannot import the tarball's test package")))
            excluded += 1
        elif x[1] != "-":
            lines.append("{:id %s :excluded %s}" % (q(rid), q("machine: its output names " + x[1])))
            excluded += 1
        else:
            lines.append("{:id %s %s}" % (q(rid), x[0]))
        if x:
            cost.append("{:id %s :seconds %s}" % (q(rid), x[2]))
    with open(out, "w", encoding="utf-8") as o:
        o.write(header + "\n".join(lines) + "\n")
    if costs:
        with open(costs, "w", encoding="utf-8") as o:
            o.write(";; The reference runs' seconds on the machine that recorded them (bin/pyi record-ref; GT8's per-program times):\n"
                    ";; machine-dependent, not a reference.\n" + "\n".join(cost) + "\n")
    print("record %s: %d entries, %d references, %d excluded" % (kind, len(lines), len(lines) - excluded, excluded))


def check(args):
    """Every reference of REF reproduced by the run's TSV (the :excluded ones skipped); exit 1 on a difference"""
    kind, ref, run = arg(args, "--kind"), arg(args, "--ref"), arg(args, "--run")
    e = entries(kind, run)
    bad = n = 0
    for l in open(ref, encoding="utf-8").read().split("\n"):
        if not l.startswith("{:id "):
            continue
        rid = l[6:l.index('"', 6)]
        body = l[len("{:id ") + len(q(rid)) + 1:-1]
        if body.startswith(":excluded"):
            continue
        n += 1
        got = e.get(rid)
        if got is None or got[0] != body:
            bad += 1
            print("DIFFERS %s\n  reference %s\n  this run  %s" % (rid, body, got[0] if got else "(not run)"))
    print("check %s: %d references, %d reproduced, %d differ" % (kind, n, n - bad, bad))
    sys.exit(1 if bad else 0)


def main():
    a = sys.argv[1:]
    {"programs": programs, "libtest": libtest, "record": record, "check": check}[a[0]](a[1:])


if __name__ == "__main__":
    main()

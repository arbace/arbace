#!/usr/bin/env python3
"""PY6i: all of Lib/test, as regrtest runs it, for pyi (bin/pyi lib-test-all, bin/pyi record-ref lib-test-all).

usage: python3 -I -S -B libtest_all.py VERB ...
  list --pypath DIR --work DIR --out FILE
      regrtest's own discovery (python3 -m test --list-tests: findtests, its SPLITTESTDIRS split into their submodules,
      test.test_asyncio.test_base_events and the like), one module a line
  refs --pypath DIR --modules FILE --work DIR --out FILE [--jobs N] [--timeout S] [--tmp-base D]
      the modules run by CPython through tools/pydump/refs.py libtest (regrtest -u none --randseed 1, its JUnit XML, the
      fixed environment), N shards at once, the TSVs joined in the list's order (refs.py record writes the references)
  fixrefs --ref FILE --root ROOT
      after refs.py record: an entry no unittest driver can name excluded, "runner: regrtest's JUnit names no test id"
      (a testcase of no name, or the number a test rebound its id attribute to: test__interpreters), and one whose
      reason names the checkout's paths or their length (AF_UNIX's limit) excluded, "machine: its reason names the
      checkout's paths"
  run --root ROOT --modules FILE --dir DIR [--jobs N] [--test-timeout S] [--module-timeout S] [--oracle LIB] [--tmp-base D]
      each module through test/python/libtest_run.py, by pyi (ROOT/bin/pyi run) or with --oracle by python3 (the
      control: PYTHONPATH LIB): DIR/MODULE.out, .err, .status in test/python/libtest_judge.ae's format. A test that
      outlives S seconds (default 300) is killed and written "CASE ID timeout ..."; a driver that dies in a test (a Go
      panic, a fatal error) is written "CASE ID crash FIRST-CAUSE"; either way the driver starts again on the module's
      other tests (its SKIPFILE the tests already reported), until DONE. A run that makes no progress, or the module's
      wall past its cap (default 3600), stops: no DONE, the judge's "crash" lines, status 124 for the cap. Modules
      already run in DIR (a .status) are kept: the run resumes. Each run's TMPDIR a short directory under D (default
      DIR/_tmp), removed after its module; after each module what the runs left in /tmp (perf-PID.map, pymp-*,
      tempfile's names) removed and logged into DIR/tmp-leftovers.log (the VM forbids /tmp). PYI_LIB_TEST_ONLY (PY7, bin/pyi
      check --only): the driver runs only the listed MODULE/TEST-ID of a module it names. PYI_LIB_TEST_DRIVER_EXE (PY7, bin/pyi
      cover): the driver compiled by pyi's AOT runs each module (on its loop), PYI_LIB_TEST_COVER/MODULE its GOCOVERDIR.
  owned --owners FILE --modules FILE [--invert]
      the modules of the list a family owns (FILE's FAMILY<TAB>REGEX lines), or with --invert the others
  table --dir DIR --summary FILE --out FILE [--owners FILE --modules FILE]
      the whole Lib/test table (Markdown): per module (a module a family owns: its row "the family's", out of the sums), imports or not with the first cause, methods equal, classed,
      failing (the judge's summary.tsv), the first cause of its failing methods; then the causes summed over modules
  controls --dir DIR --controls FILE --ref REF[,REF...] --judge CMD...
      GTS81 (a)'s negative controls: each pair's subject run judged as the reference module (its files copied under the
      reference's name into DIR/_controls/ID) by the judge with no classes; the judge must fail. Exit 1 when a control
      passes ("control passed") or was not judged; a control whose subject was not run is printed and left.
"""
import collections
import os
import re
import shutil
import subprocess
import sys
import threading
import time


def arg(args, name, default=None):
    return args[args.index(name) + 1] if name in args else default


def env(work):
    home, tmp = os.path.join(work, "home"), os.path.join(work, "tmp")
    os.makedirs(home, exist_ok=True)
    os.makedirs(tmp, exist_ok=True)
    return {"PATH": "/usr/bin:/bin", "HOME": home, "TMPDIR": tmp, "PYTHONHASHSEED": "0", "LC_ALL": "C.UTF-8",
            "TZ": "UTC", "PYTHONIOENCODING": "utf-8", "PYTHONDONTWRITEBYTECODE": "1"}


# what a module's run may leave in /tmp, which the VM forbids (test_perf_profiler's perf-PID.map, multiprocessing's
# pymp-* when TMPDIR is not seen, tempfile's own names): removed after each module and logged (DIR/tmp-leftovers.log)
TMP_LEFT = re.compile(r"^(perf-\d+\.map|pymp-\w+|@test_\d+_tmp.*)$")
TMP_BASELINE = set(os.listdir("/tmp")) if os.path.isdir("/tmp") else set()
TMP_LOCK = threading.Lock()


def tmp_guard(m, log):
    """Remove what runs left in /tmp since the runner started (the names of TMP_LEFT only); log them with the module"""
    with TMP_LOCK:
        try:
            new = sorted(set(os.listdir("/tmp")) - TMP_BASELINE)
        except OSError:
            return
        for n in new:
            if TMP_LEFT.match(n):
                p = os.path.join("/tmp", n)
                if os.path.isdir(p) and not os.path.islink(p):
                    shutil.rmtree(p, ignore_errors=True)
                else:
                    try:
                        os.unlink(p)
                    except OSError:
                        pass
                with open(log, "a", encoding="utf-8") as f:
                    f.write("%s\t%s\tremoved\n" % (m, n))
            else:
                TMP_BASELINE.add(n)  # another process's: left alone, logged once
                with open(log, "a", encoding="utf-8") as f:
                    f.write("%s\t%s\tleft (not a test's name)\n" % (m, n))


def short_tmp(base, tag):
    """A short TMPDIR for one run (multiprocessing's AF_UNIX paths fit, so it uses TMPDIR rather than /tmp)"""
    p = os.path.join(base, tag)
    shutil.rmtree(p, ignore_errors=True)
    os.makedirs(p)
    return p


def read_list(path):
    return [l.strip() for l in open(path, encoding="utf-8") if l.strip() and not l.startswith("#")]


def cmd_list(args):
    pypath, work, out = arg(args, "--pypath"), arg(args, "--work"), arg(args, "--out")
    e = dict(env(work), PYTHONPATH=os.path.abspath(pypath))
    p = subprocess.run(["python3", "-s", "-P", "-B", "-m", "test", "--list-tests", "-u", "none"], cwd=work, env=e,
                       stdin=subprocess.DEVNULL, capture_output=True, text=True, check=True)
    mods = sorted(l.strip() for l in p.stdout.split("\n") if l.strip())
    with open(out, "w", encoding="utf-8") as o:
        o.write("".join(m + "\n" for m in mods))
    print("list: %d modules as regrtest finds them" % len(mods))


def cmd_refs(args):
    pypath, mods, work, out = arg(args, "--pypath"), read_list(arg(args, "--modules")), os.path.abspath(arg(args, "--work")), arg(args, "--out")
    # the work directory absolute: refs.py runs regrtest in WORK/lt/MODULE with --junit-xml under WORK; the runs' own
    # work (their TMPDIR among it) under --tmp-base, short (default WORK), one module at a time per shard, /tmp checked
    # after each (tmp_guard)
    jobs, timeout = int(arg(args, "--jobs", "2")), arg(args, "--timeout", "900")
    tbase = os.path.abspath(arg(args, "--tmp-base", work))
    refs = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "pydump", "refs.py")
    log = os.path.join(work, "tmp-leftovers.log")
    shards = [mods[i::jobs] for i in range(jobs)]
    outs, rcs = [], []

    def shard(i, s):
        tsv = os.path.join(work, "shard%d.tsv" % i)
        outs.append(tsv)
        with open(tsv, "w", encoding="utf-8"):
            pass
        for m in s:
            w = short_tmp(tbase, "r%d" % i)
            one = os.path.join(work, "shard%d.one" % i)
            rcs.append(subprocess.call(["python3", "-I", "-S", "-B", refs, "libtest", "--pypath", pypath, "--modules", m,
                                        "--work", w, "--out", one, "--timeout", timeout]))
            with open(tsv, "a", encoding="utf-8") as o, open(one, encoding="utf-8") as f:
                o.write(f.read())
            shutil.rmtree(w, ignore_errors=True)
            tmp_guard(m, log)

    ts = [threading.Thread(target=shard, args=(i, s)) for i, s in enumerate(shards) if s]
    for t in ts:
        t.start()
    for t in ts:
        t.join()
    by_mod = collections.OrderedDict((m, []) for m in mods)
    for tsv in outs:
        for l in open(tsv, encoding="utf-8"):
            if l.strip():
                by_mod.setdefault(l.split("\t", 1)[0], []).append(l)
    with open(out, "w", encoding="utf-8") as o:
        for m in by_mod:
            o.writelines(by_mod[m])
    print("refs: %d modules by CPython in %d shards into %s" % (len(mods), len(ts), out))
    sys.exit(1 if any(rcs) else 0)


def cmd_fixrefs(args):
    ref, root = arg(args, "--ref"), os.path.abspath(arg(args, "--root"))
    lines, n_id, n_path = [], 0, 0
    for l in open(ref, encoding="utf-8"):
        m = re.match(r'\{:id "([^"/]*)/([^"]*)" (:outcome .*)\}$', l.rstrip("\n"))
        if m:
            tid, body = m.group(2), m.group(3)
            if tid == "None" or tid.isdigit():  # a doctest's id (typing.get_args) is an id
                l = '{:id "%s/%s" :excluded "runner: regrtest\'s JUnit names no test id"}\n' % (m.group(1), tid)
                n_id += 1
            elif root in body or "path too long" in body:
                l = '{:id "%s/%s" :excluded "machine: its reason names the checkout\'s paths"}\n' % (m.group(1), tid)
                n_path += 1
        lines.append(l)
    open(ref, "w", encoding="utf-8").write("".join(lines))
    print("fixrefs: %d entries of no test id and %d naming the checkout's paths excluded" % (n_id, n_path))


def last_line(text):
    ls = [l for l in text.split("\n") if l.strip() and not l.startswith(" ")]
    return ls[-1] if ls else ""


def first_cause(err, status=None):
    """A dead driver's cause: a Go panic's or fatal error's line, else the last line of its standard error"""
    ls = err.split("\n")
    for i, l in enumerate(ls):
        if l.startswith(("panic: ", "fatal error: ")):
            return l.strip()[:200]
        m = re.match(r"Execution error \((\w+)\) at (\S+) \(([^)]*)\)\.?$", l)
        if m and i + 1 < len(ls):  # Arbace's report of a Go panic in the runtime: its class, place and message
            return ("%s at %s: %s" % (m.group(1), m.group(3), ls[i + 1].strip()))[:200]
    return last_line(err)[:200] or ("the driver died, status %s" % status if status is not None else "the driver died")


def run_module(m, root, d, oracle, test_timeout, module_timeout, tmp):
    skipfile = os.path.join(d, m + ".skip")
    out_lines, err_parts, reported = [], [], set()
    done = imported_fail = False
    status, t_mod, attempts = "0", time.monotonic(), 0
    driver = os.path.join(root, "test", "python", "libtest_run.py")
    while True:
        attempts += 1
        with open(skipfile, "w", encoding="utf-8") as f:
            f.write("".join(t + "\n" for t in sorted(reported)))
        if oracle:
            cmd = ["python3", "-s", "-P", "-B", driver, m, skipfile]
            e = dict(env(os.path.join(d, "_work")), PYTHONPATH=oracle, TMPDIR=tmp)
            if os.environ.get("PYI_LIB_TEST_ONLY"):
                e["PYI_LIB_TEST_ONLY"] = os.environ["PYI_LIB_TEST_ONLY"]
        elif os.environ.get("PYI_LIB_TEST_DRIVER_EXE"):
            # the driver compiled by pyi's AOT (bin/pyi cover: built with Go's -cover), the module on its loop; each
            # module's counters in PYI_LIB_TEST_COVER/MODULE (Go writes them at the process's exit, one file a run)
            cmd = [os.environ["PYI_LIB_TEST_DRIVER_EXE"], driver, m, skipfile]
            e = dict(os.environ, TMPDIR=tmp)
            if os.environ.get("PYI_LIB_TEST_COVER"):
                cov = os.path.join(os.environ["PYI_LIB_TEST_COVER"], m)
                os.makedirs(cov, exist_ok=True)
                with open(os.path.join(cov, "id"), "w") as f:
                    f.write(m + "\n")
                e["GOCOVERDIR"] = cov
        else:
            cmd = [os.path.join(root, "bin", "pyi"), "run", driver, m, skipfile]
            e = dict(os.environ, TMPDIR=tmp)
        errpath = os.path.join(d, "%s.err.%d" % (m, attempts))
        with open(errpath, "wb") as ef:
            p = subprocess.Popen(cmd, cwd=d, env=e, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=ef,
                                 text=True, encoding="utf-8", errors="replace", start_new_session=True)
            state = {"current": None, "deadline": None, "killed": None}
            lock = threading.Lock()

            def watch():
                while p.poll() is None:
                    time.sleep(1)
                    with lock:
                        now = time.monotonic()
                        why = None
                        if now - t_mod > module_timeout:
                            why = "module"
                        elif state["deadline"] and now > state["deadline"]:
                            why = "test"
                        if why and not state["killed"]:
                            state["killed"] = why
                            try:
                                os.killpg(p.pid, 9)
                            except OSError:
                                pass

            w = threading.Thread(target=watch, daemon=True)
            w.start()
            progress = False
            for l in p.stdout:
                l = l.rstrip("\n")
                row = l.split("\t")
                with lock:
                    if row[0] == "START" and len(row) > 1:
                        state["current"], state["deadline"] = row[1], time.monotonic() + test_timeout
                    elif row[0] == "CASE" and len(row) > 1:
                        reported.add(row[1])
                        progress = True
                    elif row[0] == "DONE":
                        done = True
                    elif row[0] == "IMPORT":
                        imported_fail = True
                out_lines.append(l)
            rc = p.wait()
            w.join()
        err = open(errpath, encoding="utf-8", errors="replace").read()
        err_parts.append(err)
        os.unlink(errpath)
        status = str(rc if rc >= 0 else 128 - rc)
        if done or imported_fail:
            break
        cur = state["current"]
        if state["killed"] == "module":
            status = "124"
            break
        if cur and cur not in reported:
            if state["killed"] == "test":
                out_lines.append("CASE\t%s\ttimeout\tthe test past %d s (killed; the driver started again)" % (cur, test_timeout))
            else:
                out_lines.append("CASE\t%s\tcrash\t%s" % (cur, first_cause(err, status).replace("\t", " ")))
            reported.add(cur)
            progress = True
        if not progress or attempts > 200:
            break  # died between tests, or before the first: the judge's crash lines
    if os.path.exists(skipfile):
        os.unlink(skipfile)
    with open(os.path.join(d, m + ".out"), "w", encoding="utf-8") as f:
        f.write("".join(l + "\n" for l in out_lines))
    with open(os.path.join(d, m + ".err"), "w", encoding="utf-8") as f:
        f.write("\n".join(err_parts))
    with open(os.path.join(d, m + ".attempts"), "w") as f:
        f.write("%d %.0f\n" % (attempts, time.monotonic() - t_mod))
    with open(os.path.join(d, m + ".status"), "w") as f:
        f.write(status + "\n")
    return attempts


def cmd_run(args):
    root, mods, d = arg(args, "--root"), read_list(arg(args, "--modules")), os.path.abspath(arg(args, "--dir"))
    jobs = int(arg(args, "--jobs", "1"))
    test_timeout, module_timeout = float(arg(args, "--test-timeout", "300")), float(arg(args, "--module-timeout", "3600"))
    oracle = arg(args, "--oracle")
    tbase = os.path.abspath(arg(args, "--tmp-base", os.path.join(d, "_tmp")))
    os.makedirs(d, exist_ok=True)
    todo = [m for m in mods if not os.path.exists(os.path.join(d, m + ".status"))]
    print("lib-test-all: %d modules, %d to run (%d kept), %d at once" % (len(mods), len(todo), len(mods) - len(todo), jobs),
          flush=True)
    q, lock = list(todo), threading.Lock()

    def worker(k):
        while True:
            with lock:
                if not q:
                    return
                m = q.pop(0)
            t0 = time.monotonic()
            tmp = short_tmp(tbase, "j%d" % k)
            n = run_module(m, root, d, oracle, test_timeout, module_timeout, tmp)
            shutil.rmtree(tmp, ignore_errors=True)
            tmp_guard(m, os.path.join(d, "tmp-leftovers.log"))
            print("lib-test-all: %s run (%d s, %d driver run%s)" % (m, time.monotonic() - t0, n, "" if n == 1 else "s"),
                  flush=True)

    ts = [threading.Thread(target=worker, args=(k,)) for k in range(jobs)]
    for t in ts:
        t.start()
    for t in ts:
        t.join()


def module_cause(d, m):
    """(imports, first cause): the IMPORT line, a dead driver's cause, else the commonest failing outcome's reason"""
    p = os.path.join(d, m + ".out")
    if not os.path.exists(p):
        return False, "not run"
    rows = [l.rstrip("\n").split("\t") for l in open(p, encoding="utf-8", errors="replace")]
    for r in rows:
        if r[0] == "IMPORT":
            msg = r[1] if len(r) > 1 else "?"
            return ("skipped" if msg.startswith("SkipTest") else False), msg
    cases = [r for r in rows if r[0] == "CASE" and len(r) > 2]
    if not cases and not any(r[0] == "DONE" for r in rows):
        return False, first_cause(open(os.path.join(d, m + ".err"), encoding="utf-8", errors="replace").read())
    bad = collections.Counter()
    for r in cases:
        if r[2] in ("failure", "error", "crash", "timeout"):
            bad[norm(r[2] + " " + (r[3] if len(r) > 3 else ""))] += 1
    return True, (bad.most_common(1)[0][0] if bad else "")


def norm(s):
    s = re.sub(r"0x[0-9a-f]+", "0x…", s)
    s = re.sub(r"'[^']{40,}'", "'…'", s)
    return s[:110]


def read_owners(path):
    out = []
    for l in open(path, encoding="utf-8"):
        if l.strip() and not l.startswith("#"):
            fam, rx = l.rstrip("\n").split("\t", 1)
            out.append((fam, re.compile(rx)))
    return out


def owner(owners, m):
    return next((f for f, rx in owners if rx.search(m)), None)


def cmd_owned(args):
    owners, mods = read_owners(arg(args, "--owners")), read_list(arg(args, "--modules"))
    inv = "--invert" in args
    for m in mods:
        if (owner(owners, m) is None) == inv:
            print(m)


def cmd_table(args):
    d, summary, out = arg(args, "--dir"), arg(args, "--summary"), arg(args, "--out")
    owners = read_owners(arg(args, "--owners")) if arg(args, "--owners") else []
    rows = [l.rstrip("\n").split("\t") for l in open(summary, encoding="utf-8") if l.strip()]
    rows = [r for r in rows if owner(owners, r[0]) is None]
    owned = [(m, owner(owners, m)) for m in read_list(arg(args, "--modules"))] if arg(args, "--modules") else []
    owned = [(m, f) for m, f in owned if f]
    tot = [0, 0, 0, 0]
    noimp = collections.Counter()
    lines = ["| module | imports | methods | equal | classed | failing | first cause |", "|---|---|---:|---:|---:|---:|---|"]
    for r in sorted(rows):
        m, n, eq, cl, fa = r[0], int(r[1]), int(r[2]), int(r[3]), int(r[4])
        imp, cause = module_cause(d, m)
        if imp is False:
            noimp[cause_key(cause)] += n
        for i, v in enumerate((n, eq, cl, fa)):
            tot[i] += v
        lines.append("| %s | %s | %d | %d | %d | %d | %s |" % (m, {True: "yes", False: "no"}.get(imp, imp), n, eq, cl, fa,
                                                            cause.replace("|", "\\|")))
    for m, f in owned:
        if f.startswith("not-run"):
            lines.append("| %s | not run | | | | | by design: %s |" % (m, f.split(":", 1)[1].strip()))
        else:
            lines.append("| %s | the family's (%s) | | | | | measured by %s's own lib-test |" % (m, f, f))
    lines.append("| **total** (%d modules) | %d import | **%d** | **%d** | **%d** | **%d** | |" % (
        len(rows), sum(1 for r in rows if module_cause(d, r[0])[0] is True), tot[0], tot[1], tot[2], tot[3]))
    lines += ["", "Modules that do not import, by first cause (methods waiting; a module's SkipTest at import, as CPython's, apart):", "",
              "| first cause | modules | methods |", "|---|---:|---:|"]
    mods_by = collections.Counter()
    for r in rows:
        imp, cause = module_cause(d, r[0])
        if imp is False:
            mods_by[cause_key(cause)] += 1
    for k, v in noimp.most_common():
        lines.append("| %s | %d | %d |" % (k.replace("|", "\\|"), mods_by[k], v))
    jl = os.path.join(d, "judge.log")
    if os.path.exists(jl):
        causes, cmods = collections.Counter(), collections.defaultdict(set)
        for l in open(jl, encoding="utf-8", errors="replace"):
            mm = re.match(r"  FAIL ([^/]+)/\S+: (.*?) \(CPython \w+\)( -- no class.*)?$", l.rstrip("\n"))
            if mm:
                k = cause_key(re.sub(r"^(import|crash|stopped-in) ", r"\1: ", mm.group(2)))
                causes[k] += 1
                cmods[k].add(mm.group(1))
        lines += ["", "The failing methods' causes, summed (the 60 commonest of %d; the judge's lines, addresses and long "
                  "strings elided):" % len(causes), "", "| cause | methods | modules |", "|---|---:|---:|"]
        for k, v in causes.most_common(60):
            lines.append("| %s | %d | %d |" % (k.replace("|", "\\|"), v, len(cmods[k])))
    with open(out, "w", encoding="utf-8") as o:
        o.write("\n".join(lines) + "\n")
    print("table: %d modules, %d methods: %d equal, %d classed, %d failing -> %s" % (len(rows), *tot, out))


def cause_key(c):
    m = re.match(r"(\w+Error): No module named '([^']+)'", c)
    if m:
        return "%s: No module named '%s'" % (m.group(1), m.group(2))
    return norm(c)


def cmd_controls(args):
    d, controls, ref = arg(args, "--dir"), arg(args, "--controls"), arg(args, "--ref")
    judge = args[args.index("--judge") + 1:]
    import ast
    text = open(controls, encoding="utf-8").read()
    pairs = re.findall(r'\{:id "([^"]+)" :subject "([^"]+)" :reference "([^"]+)"', text)
    bad = 0
    for cid, subj, refm in pairs:
        if not os.path.exists(os.path.join(d, subj + ".status")):
            print("control %s: subject %s not run, left" % (cid, subj))
            continue
        cd = os.path.join(d, "_controls", cid)
        shutil.rmtree(cd, ignore_errors=True)
        os.makedirs(cd)
        for ext in ("out", "err", "status"):
            src = os.path.join(d, subj + "." + ext)
            if os.path.exists(src):
                shutil.copy(src, os.path.join(cd, refm + "." + ext))
        p = subprocess.run(judge + ["--dir", cd] + sum((["--ref", r] for r in ref.split(",")), []) + ["--no-classes", refm],
                           capture_output=True, text=True)
        summ = [l for l in p.stdout.split("\n") if l.startswith("lib-test " + refm + ":")]
        if not summ:
            print("control not judged: %s (%s against %s): %s" % (cid, subj, refm, (p.stderr or p.stdout)[-300:]))
            bad += 1
        elif p.returncode == 0:
            print("control passed: %s (%s against %s's references): %s" % (cid, subj, refm, summ[0]))
            bad += 1
        else:
            print("control %s failed as it must: %s against %s's references: %s" % (cid, subj, refm, summ[0]))
    sys.exit(1 if bad else 0)


def main():
    a = sys.argv[1:]
    {"list": cmd_list, "refs": cmd_refs, "owned": cmd_owned, "fixrefs": cmd_fixrefs, "run": cmd_run, "table": cmd_table, "controls": cmd_controls}[a[0]](a[1:])


if __name__ == "__main__":
    main()

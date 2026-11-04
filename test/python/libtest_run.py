# PY6i: Lib/test's restartable driver (bin/pyi lib-test-all; tools/pyi/libtest_all.py runs it): libtest.py's output,
# "CASE<TAB>ID<TAB>OUTCOME<TAB>REASON" per test, "START<TAB>ID" as a test starts, "IMPORT<TAB>Type: message" for a module
# that does not import, "DONE" when every test ran (test/python/libtest_judge.ae's format), and two more:
#   - SKIPFILE (argv[2], optional) names test ids, one a line, that are not run and not reported: the tests an earlier
#     run of the module reported, or crashed or timed out in. The runner restarts the driver after a crash (a Go panic
#     ends pyi's process) or a test's timeout, the rest of the module run again: one test's crash ends one test.
#   - the suite is flattened (unittest's class and module fixtures follow the flat order, as TestSuite._handleClassSetUp
#     tracks the previous class), a suite of its own run() kept whole.
#   - each line on sys.__stdout__ after a newline: a test's own output never runs into it (the judge skips blank lines).
# Run by pyi (or by python3: the oracle's control) with sys.path's Lib holding the test package. Its work is under
# __name__ == "__main__", as regrtest's: multiprocessing's spawn and forkserver children import the main script again.
import sys


def regrtest_setup(support):
    """What regrtest -u none --randseed 1 sets before a module is imported (libregrtest/setup.py's setup_tests and
    setup_process; its JUnit, audit and unraisable hooks left out)"""
    import os
    support.verbose = 0  # regrtest's default; test.support's own is 1 ("requires verbose mode" skips)
    support.failfast = False
    support.use_resources = []  # -u none
    try:
        from test.support.os_helper import FS_NONASCII, TESTFN_UNDECODABLE
        if TESTFN_UNDECODABLE and os.supports_bytes_environ:
            os.environb.setdefault(b"PYTHONREGRTEST_UNICODE_GUARD", TESTFN_UNDECODABLE)
        elif FS_NONASCII:
            os.environ.setdefault("PYTHONREGRTEST_UNICODE_GUARD", FS_NONASCII)
    except BaseException:
        pass
    try:
        import random
        random.seed(1)  # --randseed 1
    except BaseException:
        pass


def test_id(test):
    """A test's id as unittest gives it; a test that rebinds its id attribute (test__interpreters: self.id = an
    interpreter's number) by TestCase.id"""
    try:
        i = test.id()
        if isinstance(i, str):
            return i
    except BaseException:
        pass
    try:
        import unittest
        return unittest.TestCase.id(test)
    except BaseException:
        return repr(test)


def main():
    name = sys.argv[1]
    skipfile = sys.argv[2] if len(sys.argv) > 2 else None

    out = getattr(sys, "__stdout__", None) or sys.stdout

    def line(*parts):
        # on the original stdout (a test's captured_stdout does not take it), on a line of its own (a test's own output
        # without its newline, as test_concurrent_futures' timings, does not prefix it)
        out.write("\n" + "\t".join(str(p).replace("\t", " ").replace("\n", " ")[:300] for p in parts) + "\n")
        out.flush()

    skip = set()
    if skipfile:
        with open(skipfile, encoding="utf-8") as f:
            skip = set(l.rstrip("\n") for l in f if l.strip())

    try:
        from test import support
        regrtest_setup(support)
        modname = name if name.startswith("test.") else "test." + name
        mod = __import__(modname, fromlist=["*"])
        import unittest
    except BaseException as e:
        line("IMPORT", type(e).__name__ + ": " + str(e))
        return

    class Result(unittest.TestResult):
        def startTest(self, test):
            super().startTest(test)
            line("START", test_id(test))

        def addSuccess(self, test):
            line("CASE", test_id(test), "ok", "")

        def addFailure(self, test, err):
            line("CASE", test_id(test), "failure", err[1])

        def addError(self, test, err):
            msg = str(err[1])
            if test_id(test).startswith("unittest.loader._FailedTest."):
                # a submodule the loader could not import: its traceback's last line, as libtest.py (the judge's import)
                msg = [l for l in msg.splitlines() if l.strip()][-1].strip() if msg.strip() else msg
            line("CASE", test_id(test), "error", msg)

        def addSkip(self, test, reason):
            line("CASE", test_id(test), "skipped", reason)

        def addExpectedFailure(self, test, err):
            line("CASE", test_id(test), "ok", "expected failure")

        def addUnexpectedSuccess(self, test):
            line("CASE", test_id(test), "failure", "unexpected success")

        def addSubTest(self, test, subtest, err):
            super().addSubTest(test, subtest, err)
            if err is not None and test_id(test) not in self.subfailed:
                self.subfailed.add(test_id(test))
                kind = "failure" if issubclass(err[0], test.failureException) else "error"
                line("CASE", test_id(test), kind, err[1])

    Result.subfailed = set()

    # bin/pyi cover (PY7): the cover build's _pyi_cover writes the runtime's counters after each test, the test's own
    # (tools/pyi/libtest_cover.go); the module's import counted with its first test
    import os
    if os.environ.get("PYI_LIB_TEST_COVER_TESTS"):
        try:
            import _pyi_cover

            def stopTest(self, test, _flush=_pyi_cover.flush):
                unittest.TestResult.stopTest(self, test)
                _flush(name + "/" + test_id(test))
            Result.stopTest = stopTest
        except ImportError:
            pass

    _plain_runs = (unittest.TestSuite.run, unittest.BaseTestSuite.run)

    def flatten(s, out):
        if isinstance(s, unittest.BaseTestSuite) and type(s).run in _plain_runs:
            for t in s:
                flatten(t, out)
        elif isinstance(s, unittest.BaseTestSuite):
            out.append(s)  # a suite of its own run(): kept whole
        else:
            if test_id(s) not in skip:
                out.append(s)

    loader = unittest.TestLoader()
    tests = []
    flatten(loader.loadTestsFromModule(mod), tests)
    # PYI_LIB_TEST_ONLY (bin/pyi check --only, PY7): lines MODULE/TEST-ID; when it names some of this module's tests,
    # only those run (a suite of its own run() kept whole); a module it does not name runs whole
    import os
    if os.environ.get("PYI_LIB_TEST_ONLY"):
        with open(os.environ["PYI_LIB_TEST_ONLY"], encoding="utf-8") as f:
            pre = name + "/"
            only = set(l.strip()[len(pre):] for l in f if l.strip().startswith(pre))
        if only:
            tests = [t for t in tests if not isinstance(t, unittest.TestCase) or test_id(t) in only]
    # PYI_LIB_TEST_SKIP's tests (libtest.py's list, ID<TAB>REASON): run as an error naming the reason
    listed = {}
    if os.environ.get("PYI_LIB_TEST_SKIP"):
        with open(os.environ["PYI_LIB_TEST_SKIP"], encoding="utf-8") as f:
            for l in f:
                if l.strip() and not l.startswith("#"):
                    tid, _, why = l.rstrip("\n").partition("\t")
                    listed[tid] = why
    for t in tests:
        if isinstance(t, unittest.TestCase) and test_id(t) in listed:
            def not_run(why=listed[test_id(t)]):
                raise RuntimeError("pyi: not run, the runner's skip list: " + why)
            setattr(t, t._testMethodName, not_run)
    unittest.TestSuite(tests).run(Result())
    line("DONE")
    # a thread a failing test left waiting would hold the exit (threading._shutdown joins it): every line is out
    os._exit(0)


if __name__ == "__main__":
    main()

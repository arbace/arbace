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


def inline_values_stand_in(name):
    """test_class imports _testinternalcapi's has_inline_values at module level (CPython's inline values, which pyi's
    objects do not have, class :inline-values; queue 226): a stand-in for that one name, for that one module, so that
    its other tests run. The module stays absent for the rest of Lib/test, whose tests skip without it."""
    if name.endswith("test_class") and sys.implementation.name != "cpython":
        import types
        def has_inline_values(obj):
            raise NotImplementedError("pyi's objects have no inline values")
        try:
            import _testinternalcapi as m  # queue 594: pyi's own, with the names some tests need
        except ImportError:
            m = types.ModuleType("_testinternalcapi")
        if not hasattr(m, "has_inline_values"):
            m.has_inline_values = has_inline_values
        sys.modules.setdefault("_testinternalcapi", m)


def testinternalcapi_stand_in(name):
    """_testinternalcapi is by design (244-4 (a): CPython's internals, subinterpreters, its compiler's and optimizer's
    state); since queue 594 pyi has a partial one of its own (594-1 (a): the names test_interpreters, test_tracemalloc,
    test_import and test_capi's subinterpreter tests reach), which the stand-in carries. test_capi's modules and
    test_crossinterp import it at module level, so none of their methods ran (queue 465): a stand-in for their imports
    alone, for those modules alone, so that their other tests run. It answers
    nothing: an attribute it lacks raises AbsentInternal, an AttributeError (hasattr and getattr's default still see it
    absent) that is also unittest's SkipTest (a test, or a helper of test.support, that needs it skips, with the reason
    "pyi has no _testinternalcapi (by design, 244-4 (a)): NAME"); the names a module takes at its import (test_opt's
    TIER2_THRESHOLD, test_misc's pending_threadfunc in a class body, test_crossinterp's get_crossinterp_data) are
    placeholders that raise it when called or used. The module stays absent for the rest of Lib/test, whose tests
    skip without it."""
    taken = {"test_capi": ("TIER2_THRESHOLD", "pending_threadfunc"), "test_crossinterp": ("get_crossinterp_data",)}
    base = name[5:] if name.startswith("test.") else name
    base = base.split(".")[0]
    if base not in taken or sys.implementation.name == "cpython":
        return
    import types
    import unittest
    why = "pyi has no _testinternalcapi (by design, 244-4 (a)): "

    class AbsentInternal(AttributeError, unittest.SkipTest):
        pass

    class Placeholder:
        __slots__ = ("_name",)

        def __init__(self, n):
            self._name = n

        def __repr__(self):
            return "<pyi: _testinternalcapi." + self._name + " absent>"

        def _absent(self, *args, **kwargs):
            raise AbsentInternal(why + self._name)

        __call__ = __index__ = __int__ = __float__ = __bool__ = __iter__ = __len__ = __getitem__ = _absent
        __add__ = __radd__ = __sub__ = __rsub__ = __mul__ = __rmul__ = __truediv__ = __floordiv__ = __mod__ = _absent
        __lt__ = __le__ = __gt__ = __ge__ = __neg__ = _absent

    m = types.ModuleType("_testinternalcapi")
    # queue 594: pyi's own _testinternalcapi gives the names some tests need (subinterpreters, tracemalloc's): the
    # stand-in carries them, the absent ones as before
    try:
        import _testinternalcapi as real
    except ImportError:
        real = None
    if real is not None:
        for k, v in vars(real).items():
            if not (k.startswith("__") and k.endswith("__")):
                setattr(m, k, v)

    def __getattr__(attr):
        if attr.startswith("__") and attr.endswith("__"):
            raise AttributeError("module '_testinternalcapi' has no attribute '" + attr + "'")
        raise AbsentInternal(why + attr)

    m.__getattr__ = __getattr__
    for n in taken[base]:
        if not hasattr(real, n):
            setattr(m, n, Placeholder(n))
    sys.modules["_testinternalcapi"] = m


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
        inline_values_stand_in(modname)
        testinternalcapi_stand_in(modname)
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

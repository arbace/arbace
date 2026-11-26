# PY5e: Lib/test's driver for pyi (bin/pyi lib-test): the test module test.NAME imported and its tests run by unittest,
# each test's outcome printed as "CASE<TAB>ID<TAB>OUTCOME<TAB>REASON" (OUTCOME ok, skipped, failure, error, as
# regrtest's JUnit XML names them, the references' outcomes); a module that does not import prints
# "IMPORT<TAB>Type: message" (survey §8.1: one case); "START<TAB>ID" as a test starts; "DONE" when every test ran (its
# absence: the driver stopped, in the last test started, its standard error's last line the cause,
# test/python/libtest_judge.ae). Run by pyi with sys.path's Lib holding the test package.
import sys


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


# The work is under __name__ == "__main__", as libtest_run.py's (queue 249: a script's __main__ has __spec__ None, as
# CPython's, so multiprocessing's spawn children run the parent's main script again as __mp_main__).
if __name__ == "__main__":
    name = sys.argv[1]


    def line(*parts):
        sys.stdout.write("\t".join(str(p).replace("\t", " ").replace("\n", " ")[:300] for p in parts) + "\n")
        sys.stdout.flush()  # a line per test as it ends: a driver that stops keeps every line before it


    try:
        from test import support
        support.use_resources = []  # regrtest's -u none, before the module's decorators ask: no network, cpu ...
        support.verbose = 0  # regrtest without -v (its "testing with ..." prints would split the CASE lines; PY6c)
        inline_values_stand_in(name)
        testinternalcapi_stand_in(name)
        mod = __import__("test." + name, fromlist=["*"])
        import unittest
    except BaseException as e:
        line("IMPORT", type(e).__name__ + ": " + str(e))
        sys.exit(0)


    def _tid(test):
        # the test's id by its class's method: a test that rebinds its id attribute (test__interpreters: self.id = an
        # interpreter's id) still has its unittest id, so one failure does not hide the module's other results
        return type(test).id(test)


    class Result(unittest.TestResult):
        def startTest(self, test):
            super().startTest(test)
            line("START", _tid(test))  # the judge names the test a driver that stops stopped in

        def addSuccess(self, test):
            line("CASE", _tid(test), "ok", "")

        def addFailure(self, test, err):
            line("CASE", _tid(test), "failure", err[1])

        def addError(self, test, err):
            msg = str(err[1])
            if _tid(test).startswith("unittest.loader._FailedTest."):
                # a submodule the loader could not import: its traceback's last line (the judge classes its methods)
                msg = [l for l in msg.splitlines() if l.strip()][-1].strip() if msg.strip() else msg
            line("CASE", _tid(test), "error", msg)

        def addSkip(self, test, reason):
            line("CASE", _tid(test), "skipped", reason)

        def addExpectedFailure(self, test, err):
            line("CASE", _tid(test), "ok", "expected failure")

        def addUnexpectedSuccess(self, test):
            line("CASE", _tid(test), "failure", "unexpected success")

        def addSubTest(self, test, subtest, err):
            # a failing subtest fails its test, which then reports neither success nor failure itself (regrtest's
            # JUnit result names the test failed); the first failing subtest's reason, one line per test
            super().addSubTest(test, subtest, err)
            if err is not None and _tid(test) not in self.subfailed:
                self.subfailed.add(_tid(test))
                kind = "failure" if issubclass(err[0], test.failureException) else "error"
                line("CASE", _tid(test), kind, err[1])


    Result.subfailed = set()


    loader = unittest.TestLoader()
    suite = loader.loadTestsFromModule(mod)  # the module's load_tests with the standard tests, as regrtest

    # PYI_LIB_TEST_SKIP names a file of "ID<TAB>REASON" lines: tests that would hang pyi's run (a blocking wait on what
    # pyi cannot do yet, as os.fork's child), not run: each an error naming the reason, which the known file classes
    import os
    skip = {}
    if os.environ.get("PYI_LIB_TEST_SKIP"):
        with open(os.environ["PYI_LIB_TEST_SKIP"], encoding="utf-8") as f:
            for l in f:
                if l.strip() and not l.startswith("#"):
                    tid, _, why = l.rstrip("\n").partition("\t")
                    skip[tid] = why


    def mark(s):
        for t in s:
            if isinstance(t, unittest.TestSuite):
                mark(t)
            elif t.id() in skip:
                def not_run(why=skip[t.id()]):
                    raise RuntimeError("pyi: not run, the runner's skip list: " + why)
                setattr(t, t._testMethodName, not_run)


    if skip:
        mark(suite)
    suite.run(Result())
    line("DONE")
    # a thread a failing test left waiting would hold the exit (threading._shutdown joins it): every line is out
    os._exit(0)

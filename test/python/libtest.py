# PY5e: Lib/test's driver for pyi (bin/pyi lib-test): the test module test.NAME imported and its tests run by unittest,
# each test's outcome printed as "CASE<TAB>ID<TAB>OUTCOME<TAB>REASON" (OUTCOME ok, skipped, failure, error, as
# regrtest's JUnit XML names them, the references' outcomes); a module that does not import prints
# "IMPORT<TAB>Type: message" (survey §8.1: one case); "START<TAB>ID" as a test starts; "DONE" when every test ran (its
# absence: the driver stopped, in the last test started, its standard error's last line the cause,
# test/python/libtest_judge.ae). Run by pyi with sys.path's Lib holding the test package.
import sys

name = sys.argv[1]


def line(*parts):
    sys.stdout.write("\t".join(str(p).replace("\t", " ").replace("\n", " ")[:300] for p in parts) + "\n")
    sys.stdout.flush()  # a line per test as it ends: a driver that stops keeps every line before it


try:
    from test import support
    support.use_resources = []  # regrtest's -u none, before the module's decorators ask: no network, cpu ...
    mod = __import__("test." + name, fromlist=["*"])
    import unittest
except BaseException as e:
    line("IMPORT", type(e).__name__ + ": " + str(e))
    sys.exit(0)


class Result(unittest.TestResult):
    def startTest(self, test):
        super().startTest(test)
        line("START", test.id())  # the judge names the test a driver that stops stopped in

    def addSuccess(self, test):
        line("CASE", test.id(), "ok", "")

    def addFailure(self, test, err):
        line("CASE", test.id(), "failure", err[1])

    def addError(self, test, err):
        line("CASE", test.id(), "error", err[1])

    def addSkip(self, test, reason):
        line("CASE", test.id(), "skipped", reason)

    def addExpectedFailure(self, test, err):
        line("CASE", test.id(), "ok", "expected failure")

    def addUnexpectedSuccess(self, test):
        line("CASE", test.id(), "failure", "unexpected success")

    def addSubTest(self, test, subtest, err):
        # a failing subtest fails its test, which then reports neither success nor failure itself (regrtest's
        # JUnit result names the test failed); the first failing subtest's reason, one line per test
        super().addSubTest(test, subtest, err)
        if err is not None and test.id() not in self.subfailed:
            self.subfailed.add(test.id())
            kind = "failure" if issubclass(err[0], test.failureException) else "error"
            line("CASE", test.id(), kind, err[1])


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

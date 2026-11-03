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
        msg = str(err[1])
        if test.id().startswith("unittest.loader._FailedTest."):
            # a submodule the loader could not import: its traceback's last line (the judge classes its methods)
            msg = [l for l in msg.splitlines() if l.strip()][-1].strip() if msg.strip() else msg
        line("CASE", test.id(), "error", msg)

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
suite.run(Result())
line("DONE")

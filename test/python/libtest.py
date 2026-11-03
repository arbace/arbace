# PY5e: Lib/test's driver for pyi (bin/pyi lib-test): the test module test.NAME imported and its tests run by unittest,
# each test's outcome printed as "CASE<TAB>ID<TAB>OUTCOME<TAB>REASON" (OUTCOME ok, skipped, failure, error, as
# regrtest's JUnit XML names them, the references' outcomes); a module that does not import prints
# "IMPORT<TAB>Type: message" (survey §8.1: one case). Run by pyi with sys.path's Lib holding the test package.
import sys

name = sys.argv[1]


def line(*parts):
    sys.stdout.write("\t".join(str(p).replace("\t", " ").replace("\n", " ")[:300] for p in parts) + "\n")


try:
    from test import support
    support.use_resources = []  # regrtest's -u none, before the module's decorators ask: no network, cpu ...
    mod = __import__("test." + name, fromlist=["*"])
    import unittest
except BaseException as e:
    line("IMPORT", type(e).__name__ + ": " + str(e))
    sys.exit(0)


class Result(unittest.TestResult):
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


loader = unittest.TestLoader()
suite = loader.loadTestsFromModule(mod)  # the module's load_tests with the standard tests, as regrtest
suite.run(Result())

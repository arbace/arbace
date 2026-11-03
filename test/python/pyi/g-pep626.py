# PY6-core: f_lineno after a raise as CPython reports it (RERAISE N restores the frame's instruction; co_firstlineno):
# test_exceptions' PEP626Tests, run as a program (Lib/test/test_exceptions.py, CPython 3.14.10)
import unittest


class PEP626Tests(unittest.TestCase):

    def lineno_after_raise(self, f, *expected):
        try:
            f()
        except Exception as ex:
            t = ex.__traceback__
        else:
            self.fail("No exception raised")
        lines = []
        t = t.tb_next # Skip this function
        while t:
            frame = t.tb_frame
            lines.append(
                None if frame.f_lineno is None else
                frame.f_lineno-frame.f_code.co_firstlineno
            )
            t = t.tb_next
        self.assertEqual(tuple(lines), expected)

    def test_lineno_after_raise_simple(self):
        def simple():
            1/0
            pass
        self.lineno_after_raise(simple, 1)

    def test_lineno_after_raise_in_except(self):
        def in_except():
            try:
                1/0
            except:
                1/0
                pass
        self.lineno_after_raise(in_except, 4)

    def test_lineno_after_other_except(self):
        def other_except():
            try:
                1/0
            except TypeError as ex:
                pass
        self.lineno_after_raise(other_except, 3)

    def test_lineno_in_named_except(self):
        def in_named_except():
            try:
                1/0
            except Exception as ex:
                1/0
                pass
        self.lineno_after_raise(in_named_except, 4)

    def test_lineno_in_try(self):
        def in_try():
            try:
                1/0
            finally:
                pass
        self.lineno_after_raise(in_try, 4)

    def test_lineno_in_finally_normal(self):
        def in_finally_normal():
            try:
                pass
            finally:
                1/0
                pass
        self.lineno_after_raise(in_finally_normal, 4)

    def test_lineno_in_finally_except(self):
        def in_finally_except():
            try:
                1/0
            finally:
                1/0
                pass
        self.lineno_after_raise(in_finally_except, 4)

    def test_lineno_after_with(self):
        class Noop:
            def __enter__(self):
                return self
            def __exit__(self, *args):
                pass
        def after_with():
            with Noop():
                1/0
                pass
        self.lineno_after_raise(after_with, 2)

    def test_missing_lineno_shows_as_none(self):
        def f():
            1/0
        self.lineno_after_raise(f, 1)
        f.__code__ = f.__code__.replace(co_linetable=b'\xf8\xf8\xf8\xf9\xf8\xf8\xf8')
        self.lineno_after_raise(f, None)

    def test_lineno_after_raise_in_with_exit(self):
        class ExitFails:
            def __enter__(self):
                return self
            def __exit__(self, *args):
                raise ValueError

        def after_with():
            with ExitFails():
                1/0
        self.lineno_after_raise(after_with, 1, 1)


# test_missing_lineno_shows_as_none replaces co_linetable: pyi has no line table (lib-test-known.edn: code-positions)
for name in sorted(n for n in dir(PEP626Tests) if n.startswith("test_") and n != "test_missing_lineno_shows_as_none"):
    r = unittest.TestResult()
    PEP626Tests(name).run(r)
    print(name, "ok" if r.wasSuccessful() else (r.failures + r.errors)[0][1].strip().splitlines()[-1])

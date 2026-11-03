# PY6-core: the default excepthook writes to sys.stderr (captured_stderr), names a __main__ class unqualified and
# prints str(exc) by the class's own __str__ (test_exceptions' test_unhandled, test_issue45826*, test_subclass)
import io
import sys


class BrokenStr(Exception):
    def __str__(self):
        raise ValueError("no")


class Custom(Exception):
    def __str__(self):
        return "custom text"


class MySyntaxError(SyntaxError):
    pass


def report(exc):
    old = sys.stderr
    sys.stderr = io.StringIO()
    try:
        try:
            raise exc
        except BaseException:
            sys.__excepthook__(*sys.exc_info())
        return sys.stderr.getvalue()
    finally:
        sys.stderr = old


for e in (ValueError("test message"), BrokenStr("m"), Custom("m"), MySyntaxError("bad bad", ("bad.py", 1, 2, "abcdefg", 1, 7))):
    text = report(e)
    print(repr(text.splitlines()[-1]), "raise exc" in text, text.endswith("\n"))
print(report(MySyntaxError("bad bad", ("bad.py", 1, 2, "abcdefg", 1, 7))).split("\n", 3)[3])


def f():
    try:
        nonsense
    except BaseException as E:
        E.with_traceback(None)
        raise ZeroDivisionError()


try:
    f()
except ZeroDivisionError:
    old, sys.stderr = sys.stderr, io.StringIO()
    sys.__excepthook__(*sys.exc_info())
    out, sys.stderr = sys.stderr.getvalue(), old
print("nonsense" in out, "ZeroDivisionError" in out)
sys.stderr = None
sys.__excepthook__(*sys.exc_info())
sys.stderr = sys.__stderr__
print("done")

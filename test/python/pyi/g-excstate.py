# PY6-core: the exception being handled as CPython keeps it, a generator's own slot over its caller's (the exc_info
# stack), and implicit chaining of the runtime's exceptions with cycles cut (_PyErr_SetObject): test_exceptions'
# test_generator_doesnt_retain_old_exc2, test_keyerror_context, test_raise_does_not_create_context_chain_cycle
import sys


def g():
    try:
        raise ValueError
    except ValueError:
        yield repr(sys.exception())
    yield repr(sys.exception())
    yield 2


gen = g()
try:
    raise IndexError
except IndexError:
    print(next(gen))
print(next(gen))


def outer_seen():
    yield repr(sys.exception())


og = outer_seen()
try:
    raise KeyError("k")
except KeyError:
    print(next(og))

try:
    d = {}
    try:
        raise ValueError("bug")
    except Exception as exc:
        err1 = exc
        d[1]
except Exception as exc:
    err2 = exc
print(repr(err2), repr(err2.__context__), err2.__context__ is err1)
try:
    try:
        1 / 0
    except ZeroDivisionError:
        [][1]
except IndexError as e:
    print(repr(e), repr(e.__context__))


class A(Exception):
    pass


class B(Exception):
    pass


class C(Exception):
    pass


try:
    try:
        raise A
    except A as a_:
        a = a_
        try:
            raise B
        except B as b_:
            b = b_
            try:
                raise C
            except C as c_:
                c = c_
                try:
                    raise a
                except A as a2:
                    pass
except A:
    pass
print(a.__context__ is c, c.__context__ is b, b.__context__ is None)
try:
    raise ValueError
except ValueError:
    try:
        raise
    except ValueError as e:
        print("reraised", repr(e))
print(repr(sys.exception()), sys.exc_info())

# PY6a: exception attributes test_exceptions reaches: deleting the read-only ones (TypeError, not a crash)
e = Exception(1, 2)
for a in ["args", "__cause__", "__context__", "__traceback__", "__suppress_context__"]:
    try:
        delattr(e, a)
        print(a, "deleted")
    except TypeError as x:
        print(a, "TypeError", x)
e.extra = 5
del e.extra
try:
    del e.extra
except AttributeError as x:
    print("AttributeError", x)
print(e.args)
for a, v in [("__cause__", 1), ("__context__", 1), ("__suppress_context__", 1), ("__traceback__", 1), ("args", 1)]:
    try:
        setattr(e, a, v)
    except TypeError as x:
        print(a, "TypeError", x)
e.__context__ = NameError()
e.__cause__ = None
print(type(e.__context__).__name__, e.__cause__, e.__suppress_context__)
e.__suppress_context__ = False
print(e.__suppress_context__)
e = Exception(42)
e.blah = 53
e.__setstate__({'a': 1, 'b': 2})
print(e.args, e.blah, e.a, e.b)
e.__setstate__({'a': 11, 'args': (1, 2, 3), 'blah': 35})
print(e.args, e.blah, e.a, e.b)
e.__setstate__(None)
try:
    e.__setstate__(42)
except TypeError as x:
    print("TypeError", x)

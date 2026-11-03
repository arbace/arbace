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

# PY6a: a module's __annotations__ and __annotate__ (CPython 3.14's module_getsets; test_grammar's
# test_var_annot_module_semantics)
import sys
import types

x: int = 1
y: "str"

me = sys.modules[__name__]
print(sorted(me.__annotations__))
m = types.ModuleType("m")
print(m.__annotations__, "__annotations__" in m.__dict__)
print(m.__annotate__, "__annotate__" in m.__dict__)
m.__annotate__ = lambda fmt: {"a": int}
print("__annotations__" in m.__dict__, m.__annotations__)
m.__annotations__ = {"b": 2}
print(m.__annotations__, "__annotate__" in m.__dict__)
del m.__annotations__
try:
    del m.__annotations__
except AttributeError as e:
    print("AttributeError", e)
for bad in (None, 5):
    try:
        if bad is None:
            del m.__annotate__
        else:
            m.__annotate__ = bad
    except TypeError as e:
        print("TypeError", e)
m.__annotate__ = lambda fmt: [1]
try:
    m.__annotations__
except TypeError as e:
    print("TypeError", e)
import json
print(json.__annotations__)
print("__annotations__" in type(m).__dict__, "__annotate__" in type(m).__dict__)

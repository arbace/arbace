# PY6a: types.MethodType's constructor (test_fstring's test__format__lookup) and a module's repr from its file
# (test_fstring's test_global)
import types
import json


class Y:
    pass


y = Y()
m = types.MethodType(lambda self, spec: ('instance', spec), y)
print(m("s"), m.__self__ is y)
for a in [(), (1,), (len, None), (5, 1)]:
    try:
        types.MethodType(*a)
    except TypeError as e:
        print(e)
print(repr(json).startswith("<module 'json' from '"), repr(json).endswith("__init__.py'>"))

# test_descr's guards (PY6g's REQUEST for PY6-core): tp_new_wrapper's 'is not safe', update_one_slot's __new__ = X.__new__, the C level's recursion checks
for f in (lambda: object.__new__(), lambda: object.__new__(''), lambda: list.__new__(object), lambda: object.__new__(list)):
    try: f(); print("no error")
    except TypeError as e: print(e)
class C(object):
    __new__ = list.__new__
try: C(); print("no error")
except TypeError as e: print(e)
class C(list):
    __new__ = object.__new__
try: C(); print("no error")
except TypeError as e: print(e)
class D(list): pass
for f in (lambda: object.__new__(D), lambda: int.__new__(bool), lambda: object.__new__(type('X', (Exception,), {}))):
    try: print(f())
    except TypeError as e: print(e)
class A(object):
    pass
A.__call__ = A()
try: A()()
except RecursionError: print("RecursionError call")
class Foo:
    pass
Foo.__repr__ = Foo.__str__
for g in (str, repr):
    try: g(Foo())
    except RecursionError: print("RecursionError", g.__name__)
def deep(n): return n if n == 0 else deep(n - 1)
print(deep(500))
# a Go panic of the runtime is a SystemError of its instruction, not the program's end (pyi only; CPython has none)
class MyClassMethod(classmethod):
    def __init__(self, func):
        pass
try:
    repr(MyClassMethod(lambda: None))
except SystemError:
    pass
print("after")

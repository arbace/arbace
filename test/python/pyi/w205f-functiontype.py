# types.FunctionType(code, globals, name, argdefs, closure, kwdefaults) and types.CellType (queue 205f): CPython 3.14's
# Objects/funcobject.c func_new and cellobject.c cell_new, their checks and messages; a dict subclass's __missing__
# as the globals (annotationlib's FORWARDREF), an annotated typing.NamedTuple and annotationlib's formats.
import types, annotationlib, typing, re

def show(d):
    print(re.sub(r" at 0x[0-9a-f]+", " at ADDR", repr(d)))

def add(a, b=2, *, c=3):
    "add's doc"
    return a + b + c + K

K = 100
f = types.FunctionType(add.__code__, globals())  # no defaults: b required, c too
print(f(1, 2, c=0), f.__name__, f.__qualname__, f.__doc__, f.__module__, f.__defaults__, f.__kwdefaults__)
g = types.FunctionType(add.__code__, {"K": 1000}, "renamed", (5,), None, {"c": 7})
print(g(1), g.__name__, g.__qualname__, g.__module__, g.__defaults__, g.__kwdefaults__, g.__globals__)
print(g(1, 1, c=1))
h = types.FunctionType(code=add.__code__, globals={"K": 0, "__name__": "mymod"}, argdefs=(0,), kwdefaults={"c": 0})
print(h(1), h.__module__, h.__builtins__ is __builtins__.__dict__)
try:
    types.FunctionType(add.__code__, {"K": 0})(1)
except TypeError as e:
    print("TypeError", e)

def outer():
    x = 10
    y = 20
    def inner(z):
        return x + y + z
    return inner

inner = outer()
print(inner.__code__.co_freevars)
c1, c2 = types.CellType(1), types.CellType(2)
f2 = types.FunctionType(inner.__code__, globals(), None, None, (c1, c2))
print(f2(3), f2.__closure__ == (c1, c2), f2.__closure__[0] is c1)
c1.cell_contents = 5
print(f2(3))
f3 = types.FunctionType(inner.__code__, {}, closure=inner.__closure__)
print(f3(0))
empty = types.CellType()
try:
    empty.cell_contents
except ValueError as e:
    print("ValueError", e)
f4 = types.FunctionType(inner.__code__, {}, closure=(empty, c2))
try:
    f4(0)
except NameError as e:
    print("NameError", e)
print(types.CellType(1) == types.CellType(1), types.CellType(1) < types.CellType(2), empty < types.CellType(0),
      empty == types.CellType(), types.CellType(3) > empty, types.CellType(1) != types.CellType(1))
try:
    hash(empty)
except TypeError as e:
    print("TypeError", e)
print(type(empty).__name__, repr(empty).startswith("<cell at 0x"), repr(empty).endswith(": empty>"),
      repr(c2).endswith(": int object at 0x" + repr(c2).rsplit("0x", 1)[1]), " int object at " in repr(c2))

def check(*args, **kw):
    try:
        types.FunctionType(*args, **kw)
        print("no error")
    except (TypeError, ValueError) as e:
        print(type(e).__name__, e)

check()
check(add.__code__)
check(1, {})
check(add.__code__, [])
check(add.__code__, {}, 3)
check(add.__code__, {}, None, [1])
check(add.__code__, {}, None, None, [1])
check(inner.__code__, {})
check(inner.__code__, {}, None, None, None)
check(add.__code__, {}, None, None, None, [])
check(add.__code__, {}, None, None, (c1,))
check(inner.__code__, {}, None, None, (c1,))
check(inner.__code__, {}, None, None, (c1, 2))
check(add.__code__, {}, None, None, None, None, 7)
check(add.__code__, {}, bogus=1)
check(add.__code__, {}, code=add.__code__)
check(types.CellType(), {})
for args, kw in [((1, 2), {}), ((), {"x": 1})]:
    try:
        types.CellType(*args, **kw)
    except TypeError as e:
        print("TypeError", e)

class Missing(dict):
    def __missing__(self, key):
        if key.startswith("b"):
            return "<" + key + ">"
        raise KeyError(key)

def uses():
    return (alpha, len("ab"), beta)

m = Missing(alpha=1)
fm = types.FunctionType(uses.__code__, m)
print(fm(), fm.__globals__ is m)
try:
    types.FunctionType((lambda: gamma).__code__, m)()
except NameError as e:
    print("NameError", e)

class P(typing.NamedTuple):
    x: int
    y: "Undefined" = 0

print(P(1), P(1, 2), P._fields, P.__annotations__["x"], P._field_defaults)
print(annotationlib.get_annotations(P, format=annotationlib.Format.FORWARDREF))
print(annotationlib.get_annotations(P, format=annotationlib.Format.STRING))

def ann(a: int, b: Undefined2, *, c: list[Nope] = None) -> dict[str, Unknown]: pass

show(annotationlib.get_annotations(ann, format=annotationlib.Format.FORWARDREF))
print(annotationlib.get_annotations(ann, format=annotationlib.Format.STRING))
try:
    annotationlib.get_annotations(ann)
except NameError as e:
    print("NameError", e)

def mk():
    local_t = float
    def inner(a: local_t, b: missing_local) -> local_t: pass
    return inner

show(annotationlib.get_annotations(mk(), format=annotationlib.Format.FORWARDREF))
print(annotationlib.get_annotations(mk(), format=annotationlib.Format.STRING))

class Q:
    z: Q
    w: Later

print(annotationlib.get_annotations(Q, format=annotationlib.Format.FORWARDREF))
print(annotationlib.get_annotations(Q, format=annotationlib.Format.STRING))

# a function's __annotations__ assigned: __annotate__ None after it (func_set_annotations), so annotationlib's
# formats read the dict (test_annotationlib's test_stock_annotations)
def stock(a: int, b: str): pass

print(annotationlib.get_annotations(stock, format=annotationlib.Format.STRING))
stock.__annotations__ = {"a": "stock", "b": "str"}
print(stock.__annotate__, stock.__annotations__)
for fmt in (annotationlib.Format.VALUE, annotationlib.Format.FORWARDREF, annotationlib.Format.STRING):
    print(fmt, annotationlib.get_annotations(stock, format=fmt))
try:
    stock.__annotations__ = 1
except TypeError as e:
    print("TypeError", e)
del stock.__annotations__
print(stock.__annotate__, stock.__annotations__)

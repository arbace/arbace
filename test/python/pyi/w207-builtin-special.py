# queue 207: a built-in function stored as a class's special method is called without the instance (CPython's
# lookup_maybe_method binds only what has __get__), as attribute access already did; a built-in type's method put
# in a class (a method descriptor) still binds; typing.NewType's __call__ = _idfunc (where pip install stopped).
import typing, socket

UserId = typing.NewType("UserId", int)
print(UserId(5), type(UserId(5)).__name__)

class C:
    __call__ = len
    __len__ = staticmethod(lambda: 3)

try:
    print(C()("abc"))
except TypeError as e:
    print("TypeError", e)
try:
    print(C()())
except TypeError as e:
    print("TypeError", e)

class G:
    __getitem__ = abs               # a built-in function: abs(key)
print(G()[-4])

class R:
    __repr__ = object.__repr__      # a method descriptor (object's): binds
print(repr(R()).startswith("<__main__.R object"))

class L(list):
    __len__ = list.__len__          # list's method descriptor: binds to the instance
print(len(L([1, 2, 3])))

class B:
    __bytes__ = bytes               # bytes(), no instance
try:
    print(bytes(B()))
except TypeError as e:
    print("TypeError", e)

class Init:
    __init__ = print                # print(*args) without the instance
Init(1, 2)

P = typing.ParamSpec("P")
print(repr(P.args), repr(P.kwargs), P.args == P.args, P.args == P.kwargs)
print(socket.socket.__dict__.get("getsockname") is None)

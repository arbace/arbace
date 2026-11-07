# A built-in type's method taken from the class is a method descriptor: put back in a class's dict it binds to the
# instance, as test_socket's test_injected_authentication_failure does with socket.socket.getsockname; a built-in
# function there does not (queue 200, PY6-core)
import socket
orig = socket.socket.getsockname
socket.socket.getsockname = orig
s = socket.socket()
print(s.getsockname()[0])
socket.socket.getsockname = lambda self: ("patched", 0)
print(s.getsockname()[0])
socket.socket.getsockname = orig
print(s.getsockname()[0])
s.close()

class L(list):
    pass
L.app = list.append
x = L(); x.app(3); print(x)
class A:
    f = len
    up = str.upper
print(A.f([1, 2]), A().f([1, 2, 3]))
class S(str):
    up2 = str.upper
print(S("ab").up2())
class O:
    r = object.__repr__
print(O().r().startswith("<__main__.O object"))

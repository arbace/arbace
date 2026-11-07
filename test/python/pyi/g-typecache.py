# PY6-core: the type attribute cache's invalidation: a class's, a base's and a metaclass's namespace changed after
# lookups (set, replaced, deleted, added where it was absent), special methods set late, MROs with several bases
class A:
    x = 1

    def m(self):
        return "A.m"


class B(A):
    pass


class C(B):
    pass


c = C()
for _ in range(3):
    print(c.x, c.m(), C.x, hasattr(c, "y"))
A.x = 2
A.y = "new"
print(c.x, c.y, C.y)
B.x = 3
print(c.x, A.x)
del B.x
print(c.x)
A.m = lambda self: "A.m2"
print(c.m())
B.m = lambda self: "B.m"
print(c.m())
del A.y
print(hasattr(c, "y"), hasattr(C, "y"))
setattr(C, "x", 10)
print(c.x, B().x)
delattr(C, "x")
print(c.x)


class D:
    pass


d = D()
print(hasattr(d, "__len__"))
try:
    len(d)
except TypeError as e:
    print("TypeError", e)
D.__len__ = lambda self: 7
print(len(d), hasattr(d, "__len__"))
del D.__len__
try:
    len(d)
except TypeError as e:
    print("TypeError", e)


class P:
    def f(self):
        return "P"


class Q:
    def f(self):
        return "Q"

    def g(self):
        return "Q.g"


class R(P, Q):
    pass


r = R()
print(r.f(), r.g())
del P.f
print(r.f())
P.g = lambda self: "P.g"
print(r.g())


class Meta(type):
    pass


class M(metaclass=Meta):
    pass


print(hasattr(M, "hello"))
Meta.hello = "meta"
print(M.hello)
Meta.hello = "meta2"
print(M.hello)


class Prop:
    @property
    def v(self):
        return "prop"


p = Prop()
p.__dict__["v"] = "inst"
print(p.v)
Prop.v = "plain"
print(p.v)
del Prop.v
print(p.v)


class L(list):
    pass


l = L([1, 2])
print(len(l), callable(l.append))
L.append = lambda self, x: "custom"
print(l.append(3), l)

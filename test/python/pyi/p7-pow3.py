# PY7: pow(a, b, m) as ternary_op: the exponent's __rpow__ (3.14), a subclass's first, big ints (the built-in did ints by
# int64 and an object only by its __pow__)
class D:
    def __rpow__(self, other, mod=None):
        return ("rpow", other, mod)
    def __pow__(self, other, mod=None):
        return ("pow", other, mod)
print(pow(2, D(), 7), pow(D(), 2, 7), pow(2, D()))
print(pow(2**100, 3, 10**30), pow(3, -1, 7), pow(-2, 3, -5), pow(True, 2, 3))
class E(int):
    def __rpow__(self, other, mod=None):
        return ("E.rpow", other, mod)
print(pow(2, E(3), 7), pow(E(2), 3, 7))
for args in ((2, 3.0, 5), (2, 3, 5.0), ("a", 1, 2), (2, 3, 0)):
    try:
        print(pow(*args))
    except (TypeError, ValueError) as e:
        print(type(e).__name__, e)

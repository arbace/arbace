# PY5b's smoke programs: decorators apply bottom-up (the nearest first), each called on the result.
def tag(name):
    def deco(f):
        def wrapped(*args):
            return name + "(" + f(*args) + ")"
        return wrapped
    return deco

@tag("outer")
@tag("middle")
@tag("inner")
def g(x):
    return x

print(g("x"))

order = []
def record(n):
    order.append(n)
    def d(f):
        order.append("apply " + n)
        return f
    return d

@record("a")
@record("b")
def h():
    pass

print(order)

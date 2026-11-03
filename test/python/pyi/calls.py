# PY5b0's smoke programs: functions, defaults, keywords, closures, recursion, comprehensions.
def f(a, b=10, *rest, c=3, **kw):
    return a * b + c + len(rest) + len(kw)

print(f(1), f(1, 2), f(1, 2, 3, 4, c=0, d=5), f(a=2, b=3))

def counter():
    n = 0
    def inc():
        nonlocal n
        n += 1
        return n
    return inc

c = counter()
print(c(), c(), c())

def fact(n):
    return 1 if n == 0 else n * fact(n - 1)

print(fact(20))
print([i * i for i in [1, 2, 3] if i != 2], len([y for y in (5, 6)]))
for i in [1, 2]:
    for j in (3, 4):
        if j == 4:
            break
        print(i, j)
a, b = 1, 2
print(b, a)

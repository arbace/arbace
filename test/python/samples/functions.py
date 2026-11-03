# Functions, calls, parameters, lambdas, generators, closures, async (doc/python/SPEC.md §7, §6.1).
import asyncio


def cache(f):
    f.cached = True
    return f


@cache
def f[T](a: T, /, b=1, *c, d, e=2, **g) -> T:
    return (a, b, c, d, e, sorted(g.items()))


print(f(0, 1, 2, 3, d=4, z=5), f.cached)
args, kw = [1, 2], {"d": 9}
print(f(*args, **kw), f(0, d=1, e=None))

square = lambda x, *, k=2: x ** k
print(square(3), square(3, k=3))


def counter():
    n = 0

    def bump():
        nonlocal n
        n += 1
        return n
    return bump


total = 0


def add(v):
    global total
    total += v


b = counter()
b(); b()
add(5)
print(b(), total)


def gen(n):
    got = yield n
    while got:
        got = yield got - 1
    yield from range(2)


g = gen(3)
print(next(g), g.send(2), g.send(1), list(g))


async def agen():
    for i in range(3):
        yield i
        await asyncio.sleep(0)


async def main():
    out = [i async for i in agen()]
    async with asyncio.timeout(1):
        async for i in agen():
            out.append(await asyncio.sleep(0, result=i * 10))
    return out


print(asyncio.run(main()))
if (m := len(args)) > 1:
    print("walrus", m)

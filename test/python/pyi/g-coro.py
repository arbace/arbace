# PY6-core: types.coroutine's generators awaited (CO_ITERABLE_COROUTINE: gen_is_coroutine), a coroutine yielded from
# in one, and the async with's errors in GET_AWAITABLE's words (test_coroutines' test_func_*, test_await_15, test_with_7/8)
import types


@types.coroutine
def gen():
    yield 1
    return "gen-result"


async def coro():
    return "coro-result"


@types.coroutine
def wrapper():
    r = yield from coro()
    return r


async def main():
    a = await gen()
    b = await wrapper()
    return a, b


def run(c):
    try:
        while True:
            print("yielded", c.send(None))
    except StopIteration as e:
        return e.value


print(run(main()))


c = coro()


def plain():
    yield from c


try:
    run(plain())
except TypeError as e:
    print("TypeError", e)
c.close()


class CM:
    def __init__(self, enter, exit):
        self.e, self.x = enter, exit

    async def __aenter__(self):
        return self.e

    def __aexit__(self, *a):
        return self.x


class BadEnter:
    def __aenter__(self):
        return 1

    async def __aexit__(self, *a):
        pass


async def w(cm, raise_):
    async with cm:
        if raise_:
            1 / 0


for cm, r in ((CM(1, 1), False), (CM(1, 2), True), (BadEnter(), False)):
    try:
        run(w(cm, r))
    except TypeError as e:
        print("TypeError", e)
    except ZeroDivisionError:
        print("ZeroDivisionError")


async def bad():
    await 5


try:
    run(bad())
except TypeError as e:
    print("TypeError", e)

# PY5d: coroutines and async generators driven by hand (no event loop): send, throw, close, await,
# async for, asend, athrow, aclose
def show(f):
    try:
        print('->', repr(f()))
    except BaseException as e:
        print('raised', repr(e))

def run(c):
    try:
        while True:
            v = c.send(None)
            print('yielded to the loop', repr(v))
    except StopIteration as e:
        return e

async def leaf(x):
    return x * 2
async def middle(x):
    a = await leaf(x)
    b = await leaf(a)
    return a + b
print(repr(run(middle(3))))

async def failing():
    raise KeyError('k')
async def catching():
    try:
        await failing()
    except KeyError as e:
        return 'caught ' + repr(e)
print(repr(run(catching())))

c = middle(1)
show(lambda: run(c))
show(lambda: c.send(None))
c = middle(1)
show(lambda: c.send(5))
c.close()
show(lambda: middle(1).throw(ValueError('v')))
c = middle(1)
show(c.close)

async def bad_await():
    await 1
show(lambda: run(bad_await()))
w = leaf(4).__await__()
show(w.__next__)

async def agen(n):
    i = 0
    try:
        while i < n:
            got = yield i
            if got is not None:
                print('agen got', got)
            i = i + 1
    finally:
        print('agen finally')

async def consume():
    n = 0
    async for v in agen(3):
        print('async for', v)
        n = n + v
    return n
print(repr(run(consume())))

async def comp():
    return [v * 10 async for v in agen(2)]
print(repr(run(comp())))

g = agen(3)
show(lambda: run(g.__anext__()))
show(lambda: run(g.asend('x')))
show(lambda: run(g.athrow(ValueError('into agen'))))
show(lambda: run(g.__anext__()))

g = agen(3)
show(lambda: run(g.__anext__()))
show(lambda: run(g.aclose()))
show(lambda: run(g.__anext__()))
show(lambda: run(agen(1).aclose()))

async def empty():
    return
    yield
show(lambda: run(empty().__anext__()))

async def stop_in_agen():
    raise StopAsyncIteration
    yield
show(lambda: run(stop_in_agen().__anext__()))

async def bad_for():
    async for x in 1:
        pass
show(lambda: run(bad_for()))
async def bad_with():
    async with 1:
        pass
show(lambda: run(bad_with()))
def bad_sync_with():
    with 1:
        pass
show(bad_sync_with)

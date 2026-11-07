# PY6h: asyncio's pure Python paths on pyi (selectors over select's epoll, the event loop, tasks, futures)
import asyncio
import contextvars

log = []


async def worker(name, q, out):
    while True:
        item = await q.get()
        if item is None:
            q.task_done()
            return name
        out.append((name, item * 2))
        await asyncio.sleep(0)
        q.task_done()


async def agen(n):
    for i in range(n):
        await asyncio.sleep(0)
        yield i


var = contextvars.ContextVar("var", default="unset")


async def ctx_task(v):
    var.set(v)
    await asyncio.sleep(0)
    return var.get()


async def main():
    loop = asyncio.get_running_loop()
    print("loop", type(loop).__name__, type(loop._selector).__name__)
    # gather and sleep
    r = await asyncio.gather(*(asyncio.sleep(0.001 * i, result=i) for i in range(5)))
    print("gather", r)
    # queue with workers
    q = asyncio.Queue()
    out = []
    ws = [asyncio.create_task(worker("w%d" % i, q, out)) for i in range(3)]
    for i in range(10):
        await q.put(i)
    for _ in ws:
        await q.put(None)
    await q.join()
    print("workers", sorted(await asyncio.gather(*ws)), sorted(out))
    # futures and call_soon / call_later
    fut = loop.create_future()
    loop.call_soon(log.append, "soon")
    loop.call_later(0.01, fut.set_result, "later")
    print("future", await fut, log)
    # cancellation
    t = asyncio.create_task(asyncio.sleep(10))
    await asyncio.sleep(0)
    t.cancel()
    try:
        await t
    except asyncio.CancelledError:
        print("cancelled", t.cancelled())
    # wait_for and timeouts
    try:
        await asyncio.wait_for(asyncio.sleep(10), 0.01)
    except TimeoutError:
        print("wait_for timeout")
    try:
        async with asyncio.timeout(0.01):
            await asyncio.sleep(10)
    except TimeoutError:
        print("timeout ctx")
    # TaskGroup with an error
    async def boom():
        await asyncio.sleep(0)
        raise ValueError("boom")
    try:
        async with asyncio.TaskGroup() as tg:
            tg.create_task(asyncio.sleep(1))
            tg.create_task(boom())
    except* ValueError as eg:
        print("taskgroup", [str(e) for e in eg.exceptions])
    # locks, events, semaphores, conditions
    lock = asyncio.Lock()
    ev = asyncio.Event()
    sem = asyncio.Semaphore(2)
    seen = []
    async def locked(i):
        async with sem:
            async with lock:
                seen.append(i)
                await asyncio.sleep(0)
        await ev.wait()
        return i
    ts = [asyncio.create_task(locked(i)) for i in range(4)]
    await asyncio.sleep(0.01)
    ev.set()
    print("locks", await asyncio.gather(*ts), seen)
    cond = asyncio.Condition()
    async def waiter():
        async with cond:
            await cond.wait()
            return "woken"
    wt = asyncio.create_task(waiter())
    await asyncio.sleep(0)
    async with cond:
        cond.notify_all()
    print("condition", await wt)
    # async generators and comprehensions
    print("agen", [i async for i in agen(4)])
    # context variables per task
    print("context", await asyncio.gather(ctx_task("a"), ctx_task("b")), var.get())
    # as_completed and wait
    done, pending = await asyncio.wait([asyncio.create_task(asyncio.sleep(0.001, i)) for i in range(3)])
    print("wait", sorted(f.result() for f in done), len(pending))
    res = []
    for c in asyncio.as_completed([asyncio.sleep(0.03 * (3 - i), i) for i in range(3)]):
        res.append(await c)
    print("as_completed", res)
    # shield
    inner = asyncio.create_task(asyncio.sleep(0.01, "shielded"))
    outer = asyncio.shield(inner)
    outer.cancel()
    print("shield", await inner)
    # streams over a socket pair
    import socket
    a, b = socket.socketpair()
    r1, w1 = await asyncio.open_connection(sock=a)
    r2, w2 = await asyncio.open_connection(sock=b)
    w1.write(b"hello\n")
    await w1.drain()
    print("stream", await r2.readline())
    w2.write(b"bye")
    w2.close()
    await w2.wait_closed()
    print("stream eof", await r1.read())
    w1.close()
    await w1.wait_closed()
    # eager tasks
    loop.set_task_factory(asyncio.eager_task_factory)
    async def quick():
        return "eager"
    et = asyncio.create_task(quick())
    print("eager", et.done(), await et)
    loop.set_task_factory(None)
    return "done"


print(asyncio.run(main()))
# a second run, debug mode
async def tiny():
    return asyncio.get_running_loop().get_debug()
print("debug", asyncio.run(tiny(), debug=True))
with asyncio.Runner() as runner:
    print("runner", runner.run(asyncio.sleep(0, "r1")), runner.run(asyncio.sleep(0, "r2")))

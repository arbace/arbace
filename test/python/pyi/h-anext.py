# PY6h: the builtins anext and aiter (contextlib.asynccontextmanager in a TaskGroup), an exception context across awaits
import asyncio, contextlib
class CustomException(Exception): pass
async def raise_exc():
    raise CustomException
@contextlib.asynccontextmanager
async def database():
    try:
        yield
    finally:
        raise CustomException
async def main():
    task = asyncio.current_task()
    try:
        async with asyncio.TaskGroup() as tg:
            async with database():
                tg.create_task(raise_exc())
                await asyncio.sleep(1)
    except* CustomException as err:
        print(task.cancelling(), len(err.exceptions), [type(e).__name__ for e in err.exceptions])
asyncio.run(main())
async def m2():
    try:
        try:
            await asyncio.sleep(0)
            raise KeyError(1)
        except KeyError:
            await asyncio.sleep(0)
            raise ValueError(2)
    except ValueError as e:
        print("ctx", repr(e.__context__))
    try:
        try:
            1/0
        except ZeroDivisionError:
            await asyncio.sleep(0)
            raise
    except ZeroDivisionError as e:
        print("reraise ok")
asyncio.run(m2())
async def ag():
    yield 1
    yield 2
async def m3():
    g = ag()
    print(await anext(g), await anext(g), await anext(g, "dflt"))
    try: await anext(g)
    except StopAsyncIteration: print("stop")
    it = aiter(ag()); print(type(it).__name__, [x async for x in it])
    class A:
        def __aiter__(self): return self
        async def __anext__(self): raise StopAsyncIteration
    print(await anext(A(), 7))
    try: anext(1)
    except TypeError as e: print(e)
asyncio.run(m3())

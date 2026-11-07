# PY6-core: coroutine origin tracking (sys.set_coroutine_origin_tracking_depth, cr_origin), throw() into an exhausted
# coroutine, async for's invalid awaitable from __anext__ with its cause, and unpacking's errors from __iter__
# (test_coroutines' test_origin_tracking, test_func_17/18, test_for_11, test_for_assign_raising_stop_async_iteration_2)
import os
import sys


async def corofn():
    pass


def strip(origin):
    return tuple((os.path.basename(f), l, n) for f, l, n in origin)


print(sys.get_coroutine_origin_tracking_depth())
c = corofn()
print(c.cr_origin)
c.close()
sys.set_coroutine_origin_tracking_depth(1)
c = corofn()
print(strip(c.cr_origin))
c.close()
sys.set_coroutine_origin_tracking_depth(2)


def nested():
    return corofn()


c = nested()
print(strip(c.cr_origin))
c.close()
try:
    sys.set_coroutine_origin_tracking_depth(-1)
except ValueError as e:
    print("ValueError", e)
print(sys.get_coroutine_origin_tracking_depth())
sys.set_coroutine_origin_tracking_depth(0)


async def spam():
    return "spam"


c = spam()
try:
    c.send(None)
except StopIteration as e:
    print("StopIteration", e.value)
for act in (lambda: c.send(None), lambda: c.throw(Exception("wat"))):
    try:
        act()
    except RuntimeError as e:
        print("RuntimeError", e)
c.close()
c.close()


class F:
    def __aiter__(self):
        return self

    def __anext__(self):
        return self

    def __await__(self):
        1 / 0


async def main():
    async for _ in F():
        pass


try:
    main().send(None)
except TypeError as e:
    print("TypeError", e, type(e.__cause__).__name__)


class BadIterable:
    def __iter__(self):
        raise StopAsyncIteration(42)


try:
    a, b = BadIterable()
except StopAsyncIteration as e:
    print("StopAsyncIteration", e.args)
try:
    a, b = 5
except TypeError as e:
    print("TypeError", e)

# PY5d: RecursionError at the limit through generators, coroutines and yield from; what is left usable
def deep(n):
    return deep(n + 1)
try:
    deep(0)
except RecursionError as e:
    print('RecursionError', e)

def nested_gen(n):
    yield from nested_gen(n + 1)
try:
    for x in nested_gen(0):
        pass
except RecursionError as e:
    print('generators', e)

async def nested_coro(n):
    return await nested_coro(n + 1)
c = nested_coro(0)
try:
    c.send(None)
except RecursionError as e:
    print('coroutines', e)
try:
    c.send(None)
except RuntimeError as e:
    print('after', e)

def gen_recursing():
    yield 1
    deep(0)
    yield 2
g = gen_recursing()
print(g.__next__())
try:
    g.__next__()
except RecursionError as e:
    print('in a generator', e)
try:
    g.__next__()
except StopIteration as e:
    print('finished', repr(e))

def count(n):
    return 0 if n == 0 else 1 + count(n - 1)
def recover():
    try:
        deep(0)
    except RecursionError:
        return count(900)
print(recover())
def gen_depth(n):
    if n:
        yield from gen_depth(n - 1)
    else:
        yield 'bottom'
print([v for v in gen_depth(400)])

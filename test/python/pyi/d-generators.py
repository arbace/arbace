# PY5d: generators: send, throw, close, return values, PEP 479, yield from
def show(f):
    try:
        print('->', repr(f()))
    except BaseException as e:
        print('raised', repr(e))

def counter(n):
    i = 0
    while i < n:
        got = yield i
        if got is not None:
            print('sent', got)
        i = i + 1
    return 'done'

for v in counter(3):
    print('for', v)
it = counter(3)
show(it.__next__)
show(lambda: it.send('a'))
show(lambda: it.send('b'))
show(lambda: it.send('c'))
show(lambda: it.send('d'))
show(lambda: counter(2).send(5))

def catcher():
    while True:
        try:
            yield 'ready'
        except ValueError as e:
            print('caught', repr(e))
it = catcher()
show(it.__next__)
show(lambda: it.throw(ValueError('x')))
show(lambda: it.throw(ValueError))
show(lambda: it.throw(KeyError('k')))
show(lambda: it.send(1))
show(lambda: catcher().throw(KeyError('fresh')))
show(lambda: catcher().throw(1))

def with_finally():
    try:
        yield 1
        yield 2
    finally:
        print('finally')
it = with_finally()
show(it.__next__)
show(it.close)
show(it.close)
show(with_finally().close)

def returns_on_exit():
    try:
        yield 1
    except GeneratorExit:
        print('exit')
        return 'r'
it = returns_on_exit(); it.__next__(); show(it.close)

def reraises_exit():
    try:
        yield 1
    except GeneratorExit:
        print('exit')
        raise
it = reraises_exit(); it.__next__(); show(it.close)

def ignores_exit():
    try:
        yield 1
    finally:
        yield 2
it = ignores_exit(); it.__next__(); show(it.close)

def stop_inside():
    raise StopIteration('inside')
    yield
try:
    stop_inside().__next__()
except RuntimeError as e:
    print('RuntimeError', e)

def reentrant():
    yield me.__next__()
me = reentrant()
show(me.__next__)

def inner():
    got = yield 'i1'
    print('inner got', got)
    try:
        yield 'i2'
    except KeyError as e:
        print('inner caught', repr(e))
        yield 'i3'
    return 'inner-done'
def outer():
    r = yield from inner()
    print('outer got', r)
    yield 'o1'
it = outer()
show(it.__next__)
show(lambda: it.send('s'))
show(lambda: it.throw(KeyError('t')))
show(it.__next__)
show(it.__next__)
it = outer(); it.__next__(); it.__next__()
show(it.close)

def from_list():
    r = yield from [1, 2, 3]
    print('list gave', r)
    yield from (x + 10 for x in [1, 2])
print([v for v in from_list()])

def deep(n):
    if n:
        yield n
        yield from deep(n - 1)
print([v for v in deep(5)])
print(sum_ if False else [x * x for x in (y for y in [1, 2, 3] if y != 2)])

def gen_exc_state():
    try:
        raise KeyError('a')
    except KeyError:
        yield 1
        raise
it = gen_exc_state()
show(it.__next__)
show(it.__next__)

# PY5d: finally paths: return, break and continue through finally, finally overriding, exceptions
# replaced in finally (return in finally left out: CPython 3.14 warns, PEP 765), finally run by a generator's close and by an exception thrown in
def f1():
    try:
        return 'try'
    finally:
        print('finally runs')
print(f1())

def f2():
    for i in [0, 1, 2]:
        try:
            if i == 1:
                continue
            if i == 2:
                break
            print('body', i)
        finally:
            print('finally', i)
    return 'loop done'
print(f2())


def f4():
    try:
        try:
            raise KeyError('first')
        finally:
            raise ValueError('second')
    except ValueError as e:
        return repr(e)
print(f4())

def f5():
    out = 'start'
    try:
        try:
            return 'inner'
        finally:
            print('inner finally')
    finally:
        print('outer finally')
print(f5())

def f6():
    while True:
        try:
            try:
                break
            finally:
                print('break through inner')
        finally:
            print('break through outer')
    return 'after while'
print(f6())

def g():
    try:
        try:
            yield 1
        finally:
            print('gen inner finally')
    finally:
        print('gen outer finally')
it = g()
print(it.__next__())
it.close()
it = g()
print(it.__next__())
try:
    it.throw(KeyError('thrown'))
except KeyError as e:
    print('out', repr(e))

def h():
    try:
        yield 1
    except GeneratorExit:
        return 'returned on close'
it = h()
it.__next__()
print(repr(it.close()))

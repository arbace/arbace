# queue 227: the built-in iterators' __reduce__ and __setstate__ at each stage of their life
import pickle, copy, collections, sys, re
def st(o):
    r = o.__reduce__()
    return r
def show(label, f):
    try:
        print(label, re.sub(r'0x[0-9a-f]+', '0x', str(f())))
    except BaseException as e:
        print(label, type(e).__name__, str(e).replace('collections._deque', '_deque'))
def norm(r):
    # iterators inside a reduce shown by their contents
    if isinstance(r, tuple):
        return tuple(norm(x) for x in r)
    if hasattr(r, '__next__'):
        return (type(r).__name__, list(copy.copy(r)) if False else '<it>')
    return r
class Seq:
    def __init__(s, n): s.n = n
    def __getitem__(s, i):
        if i >= s.n: raise IndexError
        return i
    def __eq__(s, o): return isinstance(o, Seq) and s.n == o.n
    __hash__ = None
def counter():
    c = [-1]
    def f():
        c[0] += 1
        return c[0]
    return f
def mk():
    yield 'list', lambda: iter([1, 2, 3])
    yield 'tuple', lambda: iter((1, 2, 3))
    yield 'str', lambda: iter('abc')
    yield 'ustr', lambda: iter('a€c')
    yield 'bytes', lambda: iter(b'abc')
    yield 'bytearray', lambda: iter(bytearray(b'abc'))
    yield 'range', lambda: iter(range(1, 10, 3))
    yield 'nrange', lambda: iter(range(10, 1, -3))
    yield 'rrange', lambda: reversed(range(1, 10, 3))
    yield 'lrange', lambda: iter(range(1 << 70, (1 << 70) + 9, 3))
    yield 'rlrange', lambda: reversed(range(1 << 70, (1 << 70) + 9, 3))
    yield 'rlist', lambda: reversed([1, 2, 3])
    yield 'rtuple', lambda: reversed((1, 2, 3))
    yield 'rstr', lambda: reversed('abc')
    yield 'set', lambda: iter({1})
    yield 'fset', lambda: iter(frozenset({1}))
    yield 'dict', lambda: iter({1: 2, 3: 4})
    yield 'seq', lambda: iter(Seq(3))
    yield 'call', lambda: iter(counter(), 2)
    yield 'deque', lambda: iter(collections.deque([1, 2, 3]))
    yield 'rdeque', lambda: reversed(collections.deque([1, 2, 3]))
    yield 'enum', lambda: enumerate((1, 2, 3), 5)
    yield 'bigenum', lambda: enumerate((1, 2, 3), 1 << 70)
for name, f in mk():
    it = f()
    print(name, type(it).__name__, type(it).__module__)
    for stage in range(5):
        show('  %d' % stage, lambda: type(it.__reduce__()[0]).__name__ + ' ' + repr(it.__reduce__()[1:]).replace(' at 0x', '@')[:0] + repr([repr(x)[:40].split(' at ')[0] if not hasattr(x, '__next__') and not isinstance(x, Seq) else type(x).__name__ for x in it.__reduce__()[1:]]))
        for p in (0, 2, 5):
            if name not in ('call', 'seq'):
                show('   p%d' % p, lambda: list(pickle.loads(pickle.dumps(it, p))))
        show('   copy', lambda: type(copy.copy(it)).__name__)
        try:
            print('   next', next(it))
        except StopIteration:
            print('   stop')
    for s in (-5, -1, 0, 1, 99, 1 << 70, 'x', None):
        g = f()
        show('  set %r' % (s,), lambda: (g.__setstate__(s), list(g)))
    show('  nosetstate', lambda: f().__setstate__())
    show('  reduceargs', lambda: f().__reduce__(1))
# zip, map, filter
z = zip([1, 2], [3, 4, 5], strict=True)
print(z.__reduce__()[2:], type(z.__reduce__()[1]))
z2 = zip([1], [2])
print(z2.__reduce__()[2:]); z2.__setstate__(True); print(z2.__reduce__()[2:]); z2.__setstate__(0); print(z2.__reduce__()[2:])
m = map(int, ['1'], strict=True)
print(m.__reduce__()[0], m.__reduce__()[1][0], m.__reduce__()[2:])
m2 = map(int, ['1']); m2.__setstate__(1); print(m2.__reduce__()[2:])
for proto in range(6):
    print(list(pickle.loads(pickle.dumps(map(str, [1, 2]), proto))), list(pickle.loads(pickle.dumps(zip('ab', [1, 2]), proto))),
          list(pickle.loads(pickle.dumps(filter(None, [0, 1]), proto))), list(pickle.loads(pickle.dumps(filter(bool, [0, 1]), proto))),
          list(pickle.loads(pickle.dumps(enumerate('ab', 3), proto))))
try:
    list(pickle.loads(pickle.dumps(zip([1], [2, 3], strict=True), 2)))
except ValueError as e:
    print(e)
print(filter(bool, []).__reduce__()[1][0], filter(None, []).__reduce__()[1][0], filter(len, []).__reduce__()[1][0])
class E(enumerate): pass
class Z(zip): pass
class M(map): pass
class F(filter): pass
e = E([1]); e.x = 1
print(e.__reduce__()[0].__name__, e.__reduce_ex__(2)[0].__name__, len(e.__reduce__()))
print(Z([1]).__reduce__()[0].__name__, M(int, []).__reduce__()[0].__name__, F(None, []).__reduce__()[0].__name__)
d = collections.deque([1, 2, 3]); i = iter(d); next(i); d.append(4)
print(i.__reduce__()[1][1]); show('mut', lambda: next(i)); print(i.__reduce__()[1][1])
d = collections.deque([1, 2, 3]); i = collections._deque_iterator(d, 1); print(list(i))
show('dq5', lambda: list(collections._deque_iterator(d, 5)))
print(type(reversed(d))(d, 1).__reduce__()[1][1], list(type(reversed(d))(d, 1)))
show('dqbad', lambda: collections._deque_iterator([1]))
class D(collections.deque): pass
print(type(iter(D([1])).__reduce__()[1][0]).__name__, type(reversed(D([1])).__reduce__()[1][0]).__name__)
print(hasattr(list, '__reversed__'), hasattr(range, '__reversed__'), hasattr(tuple, '__reversed__'), hasattr(str, '__reversed__'))
print(reversed.__reduce__ is not None, reversed([]).__length_hint__(), reversed([1, 2]).__length_hint__())
l = [1, 2, 3]; r = reversed(l); next(r); l.clear(); print(r.__length_hint__(), list(r), r.__reduce__()[1])
l = [1, 2, 3]; r = reversed(l); l.append(4); print(list(r))

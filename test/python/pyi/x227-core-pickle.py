# queue 227: subclasses of the core types through pickle and copy, array's state, deque's reduce, method descriptors
import array, collections, copy, itertools, pickle, sys
class S(set):
    def __init__(self, *a):
        super().__init__(*a)
        self.x = 1
class SK(set):
    def __init__(self, a, b=0):
        set.__init__(self, a)
        self.b = b
class F(frozenset): pass
class B(bytearray): pass
class D(collections.deque): pass
class A(array.array): pass
class L(list):
    def __new__(cls, seq, newarg=None):
        return super().__new__(cls, seq)
for proto in range(6):
    for o in (S([1]), F([1]), B(b'a'), SK([2], 3), D([1, 2], 3)):
        r = pickle.loads(pickle.dumps(o, proto))
        print(proto, repr(r), type(r).__name__, r.__dict__)
    a = A('i', [1, 2]); a.x = 10
    r = pickle.loads(pickle.dumps(a, proto))
    print(proto, r, r.__dict__, type(r).__name__)
    d = collections.deque([1]); d.append(d)
    r = pickle.loads(pickle.dumps(d, proto))
    print(proto, r[1] is r, len(r))
    for m in (list.append, str.upper, object.__init__, dict.__len__):
        print(proto, pickle.loads(pickle.dumps(m, proto)) is m or pickle.loads(pickle.dumps(m, proto)) == m)
    for c in (itertools.count(1), itertools.chain([1]), itertools.repeat(1)):
        try:
            pickle.dumps(c, proto)
        except TypeError as e:
            print(proto, e)
print(None.__reduce_ex__(2)[1:], collections.deque([1], 2).__reduce__()[1:3], collections.deque().__reduce__()[1:3])
print(type(copy.copy(D([1], 5))).__name__, copy.copy(D([1], 5)).maxlen, D([1]).copy().__class__.__name__)
for o in (memoryview(b'a'), (lambda: 0).__code__):
    try:
        o.__reduce_ex__(2)
    except TypeError as e:
        print(e)
print(bytearray.maketrans(b'a', b'b')[97], L([1], newarg=2), len(set([1]).__reduce__()))
s = set(); s.__init__([1]); s.__init__([2, 3]); print(sorted(s))
try:
    set([1], [2])
except TypeError as e:
    print(e)
try:
    set(a=1)
except TypeError as e:
    print(e)
it = itertools.tee([1, 2, 3])[0]; c = copy.copy(it); print(next(it), next(c), next(c), next(it))
print(list(itertools.islice(itertools.count(), 1, 10, sys.maxsize)), list(itertools.islice(range(10), 5, 3)))
def f(): pass
try:
    f(**1)
except TypeError as e:
    print(e)
class Sp:
    def __eq__(self, o):
        s.clear()
        return False
    def __hash__(self): return 1
s = {Sp()}; print(Sp() in s, len(s))
class Bad:
    def __iter__(self): raise ZeroDivisionError
try:
    1 in Bad()
except ZeroDivisionError:
    print('zero')

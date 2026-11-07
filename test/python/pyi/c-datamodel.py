# pyi PY5c: the data model on PY5c's runtime alone (no decorators, no star arguments, no PY5b/PY5e builtins)

def show(label, thunk):
    try:
        print(label, repr(thunk()))
    except Exception as e:
        print(label, type(e).__name__, e)

class A:
    def who(self): return 'A'
class B(A):
    def who(self): return 'B>' + super().who()
class C(A):
    def who(self): return 'C>' + super().who()
class D(B, C):
    def who(self): return 'D>' + super().who()
show('mro', lambda: [k.__name__ for k in D.__mro__])
show('super', lambda: (D().who(), super(B, D()).who()))
class X: pass
class Y: pass
class P(X, Y): pass
class Q(Y, X): pass
def bad():
    class R(P, Q): pass
show('mro-conflict', bad)
class Meta(type):
    def __new__(mcls, name, bases, ns):
        cls = super().__new__(mcls, name, bases, ns)
        cls.tag = name + '!'
        return cls
    def __call__(cls, v):
        print('meta call', cls.__name__, v)
        return super().__call__(v)
    def describe(cls): return 'class ' + cls.__name__
class M(metaclass=Meta):
    def __init__(self, v): self.v = v
class N(M): pass
show('meta', lambda: (type(M).__name__, M.tag, N.tag, M.describe(), N(5).v))
show('notonmeta', lambda: M(1).describe())
class Named:
    def __set_name__(self, owner, name): self.where = owner.__name__ + '.' + name
class S:
    a = Named()
show('set_name', lambda: S.a.where)
class Base:
    def __init_subclass__(cls, kind=None):
        cls.kind = kind
class K1(Base, kind='one'): pass
class K2(Base): pass
show('init_subclass', lambda: (K1.kind, K2.kind))
class NoArgs: pass
show('noargs', lambda: NoArgs(1))
class RetInit:
    def __init__(self): return 1
show('retinit', lambda: RetInit())
T = type('T', (A,), {'x': 1})
show('type3', lambda: (T.__name__, T.x, T().who(), T.__bases__, T.__module__, type(T()) is T))
class Outer:
    class Inner:
        "doc"
        def m(self): pass
show('names', lambda: (Outer.Inner.__qualname__, Outer.Inner.m.__qualname__, Outer.Inner.__doc__, A.__doc__))
class Data:
    def __get__(self, obj, typ=None): return ('data', obj is None, typ.__name__)
    def __set__(self, obj, v): print('set', v)
class NonData:
    def __get__(self, obj, typ=None): return 'nondata'
class E:
    d = Data()
    n = NonData()
    def m(self): return 'm'
e = E()
e.d = 5
e.n = 'inst-n'
show('descr', lambda: (e.d, e.n, E.d, E.n, e.m(), E.m(e), e.m.__self__ is e))
class Pr:
    def getx(self): return self._x
    def setx(self, v): self._x = v * 10
    x = property(getx, setx)
    ro = property(getx)
p = Pr()
p.x = 2
show('property', lambda: (p.x, p._x))
def setro(): p.ro = 1
show('property-ro', setro)
class Sl:
    __slots__ = ('a', 'b')
    def __init__(self): self.a = 1
s = Sl()
def setc(): s.c = 1
show('slots', lambda: (s.a, type(Sl.a).__name__, Sl.__slots__))
show('slot-unset', lambda: s.b)
show('slot-new', setc)
class K:
    def make(cls): return cls.__name__
    make = classmethod(make)
    def add(a, b): return a + b
    add = staticmethod(add)
class KS(K): pass
show('cm-sm', lambda: (K.make(), KS.make(), KS().make(), K.add(1, 2), K().add(3, 4)))
class GA:
    x = 1
    def __getattr__(self, name): return 'missing:' + name
show('getattr', lambda: (GA().x, GA().yz))
class GC:
    def __init__(self): object.__setattr__(self, 'log', ())
    def __setattr__(self, n, v): object.__setattr__(self, 'log', (self.log, n, v))
g = GC()
g.a = 1
show('setattr', lambda: g.log)
class Ops:
    def __init__(self, v): self.v = v
    def __repr__(self): return 'Ops(' + repr(self.v) + ')'
    def __add__(self, o): return Ops(('add', o))
    def __radd__(self, o): return Ops(('radd', o))
    def __eq__(self, o): return 'eq'
    def __lt__(self, o): return 'lt'
    def __neg__(self): return Ops('neg')
    def __len__(self): return 3
    def __getitem__(self, i):
        if i >= 3: raise IndexError(i)
        return i * 10
    def __contains__(self, x): return x == 'in'
    def __call__(self, a): return ('called', a)
    def __hash__(self): return 7
class Sub(Ops):
    def __radd__(self, o): return 'sub-radd'
show('ops', lambda: (Ops(1) + 2, 3 + Ops(1), Ops(1) + Sub(2), Ops(1) == 2, Ops(1) < 2, 2 > Ops(1), -Ops(1)))
show('protocols', lambda: (len(Ops(1)), [x for x in Ops(1)], 'in' in Ops(1), 'x' in Ops(1), Ops(1)(5), {Ops(1): 1}))
show('unsupported', lambda: Ops(1) - 1)
class NoHash:
    def __eq__(self, o): return True
show('hashnone', lambda: NoHash.__hash__)
class Later: pass
o = Later()
Later.__len__ = lambda self: 7
show('assign-dunder', lambda: len(o))
class Ex(Exception):
    def __init__(self, msg, code):
        super().__init__(msg)
        self.code = code
def raise_ex():
    try:
        raise Ex('boom', 7)
    except Exception as e:
        return (e.args, e.code, e.__cause__, type(e).__mro__[1].__name__)
show('exc', raise_ex)
n, w = 42, 'py'
show('format', lambda: (format(n, '05'), format(n, '#x'), format(-1234567, ','), format(w, '*^6'), format(w, '.1')))
show('fstr', lambda: f'{n} {w!r} {w!a} {n:>{n // 10}} {n=} {"é"!a}')
class F:
    def __format__(self, spec): return 'F<' + spec + '>'
show('dunder-format', lambda: (format(F(), 'ab'), f'{F():x}'))
show('format-err', lambda: (format(n, 's')))
show('ga', lambda: (list[int], tuple[int, ...], dict[str, list[int]]))
type Alias = tuple[int, int]
show('alias', lambda: (Alias.__name__, Alias.__value__))
def gen[T](x: T) -> T: return x
show('tparams', lambda: ([p.__name__ for p in gen.__type_params__], gen(3), gen.__annotations__))

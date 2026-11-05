# 205g: typing.Generic's subscription as CPython's (typing._generic_class_getitem, _generic_init_subclass,
# _Py_subscript_generic): several bases, ABC subscripts, Protocol, __orig_bases__, __parameters__, errors.
import typing, types
from typing import Generic, TypeVar, Protocol, MutableMapping, Mapping, Iterator, ParamSpec, TypeVarTuple, Unpack
import collections.abc as cabc

K = TypeVar("K"); V = TypeVar("V"); T = TypeVar("T"); P = ParamSpec("P"); Ts = TypeVarTuple("Ts")

def show(label, f):
    try:
        print(label, f())
    except Exception as e:
        print(label, type(e).__name__, e)

a = Generic[K, V]
print(type(a).__name__, type(a).__module__, a, a.__origin__ is Generic, a.__args__, a.__parameters__)
print(a == Generic[K, V], hash(a) == hash(Generic[K, V]), a.__mro_entries__((a, int)), a.__mro_entries__((a,)))

class RecentlyUsedContainer(typing.Generic[K, V], typing.MutableMapping[K, V]):
    def __init__(self): self.d = {}
    def __getitem__(self, k): return self.d[k]
    def __setitem__(self, k, v): self.d[k] = v
    def __delitem__(self, k): del self.d[k]
    def __iter__(self): return iter(self.d)
    def __len__(self): return len(self.d)

R = RecentlyUsedContainer
print([c.__name__ for c in R.__mro__])
print(R.__orig_bases__, R.__parameters__)
r = R[int, str]()
r[1] = "a"; print(dict(r), type(r).__name__, r.__orig_class__)
print(R[int, str], R[int, str].__origin__ is R, R[int, str].__args__)

class Box(Generic[T]):
    pass
print(Box.__mro__, Box.__orig_bases__, Box.__parameters__, Box[int], Box[int]().__orig_class__)
class IntBox(Box[int]): pass
print(IntBox.__mro__, IntBox.__orig_bases__, IntBox.__parameters__)
class Sub(Box[T], Mapping[T, V]): pass
print([c.__name__ for c in Sub.__mro__], Sub.__parameters__)

class M(cabc.MutableMapping[K, V], Generic[V, K]): pass
print(M.__parameters__, [c.__name__ for c in M.__mro__])

class Proto(Protocol[T]):
    def meth(self) -> T: ...
print(Proto.__parameters__, Proto.__orig_bases__, Proto[int], [c.__name__ for c in Proto.__mro__])

class PS(Generic[P]): pass
print(PS.__parameters__, PS[int, str], PS[[int], ]) 
class VT(Generic[Unpack[Ts]]): pass
print(VT.__parameters__, VT[int, str])
class VT2(Generic[*Ts]): pass
print(VT2.__parameters__, VT2[int])

class N[X, Y](dict):
    pass
print(N.__orig_bases__, N.__parameters__, N.__type_params__, [c.__name__ for c in N.__mro__], N[int, str])

show("empty", lambda: Generic[()])
show("plain", lambda: Generic[int])
show("dup", lambda: Generic[T, T])
show("notparams", lambda: Generic[T, int])
show("nogeneric", lambda: types.new_class("X", (Generic,)))
show("unlisted", lambda: types.new_class("X", (Generic[T], Mapping[K, V],)))
show("twice", lambda: types.new_class("X", (Generic[T], Generic[K],)))
show("toomany", lambda: Box[int, str])
show("dupbase", lambda: types.new_class("X", (Generic, Generic[T],)))
show("getitem-call", lambda: Generic.__class_getitem__(T))
show("noargs", lambda: Generic.__class_getitem__())
show("proto-int", lambda: Protocol[int])
print(typing.get_args(Box[int]), typing.get_origin(Box[int]), typing.get_origin(Generic[T]))

# PEP 695 with a TypeVarTuple (unpack_typevartuples), *Ts as typing.Unpack, defaults and substitution
class Q[A, *Zs]:
    pass
print(Q.__orig_bases__, Q.__parameters__, Q[int], Q[int, str, bytes], [*Ts], tuple[*Ts], type([*Ts][0]).__name__)
D = TypeVar("D", default=int)
class WithDefault(Generic[T, D]): pass
print(WithDefault[str], WithDefault[str, bytes])
show("toofew", lambda: WithDefault[()])
show("subst-bare", lambda: Ts.__typing_subst__(int))
show("subst-tv", lambda: T.__typing_subst__(int))
show("subst-bad", lambda: T.__typing_subst__(3))
show("prepare", lambda: T.__typing_prepare_subst__(Box, ()))
show("prepare-ok", lambda: T.__typing_prepare_subst__(Box, (int,)))
show("prepare-ps", lambda: P.__typing_prepare_subst__(PS, (int, str)))
show("prepare-arity", lambda: T.__typing_prepare_subst__(Box))
show("subst-arity", lambda: T.__typing_subst__())

# *list[int]: types.GenericAlias' iterator (ga_iter), one starred copy, its repr and equality
print((*tuple[int],)[0], tuple[*tuple[int, ...]], tuple[Unpack[tuple[*tuple[int, ...]]]], Unpack[tuple[int]])
x = (*tuple[int, str],)[0]
print(x.__origin__, x.__args__, x == tuple[int, str], x == (*tuple[int, str],)[0], list(iter(list[int])), next(iter(dict[str, int])))
print(tuple[()], *tuple[()])

# queue 232: _operator's functions are built-in functions (Modules/_operator.c), so one stored in a class is called
# without the instance (glob's _StringGlobber.concat_path = operator.add: pathlib's recursive glob); BaseExceptionGroup's
# __class_getitem__ (pytest's RaisesGroup)
import operator, os, pathlib, tempfile

print(type(operator.add).__name__, operator.add.__module__, operator.add, operator.__lt__ is operator.lt)


class C:
    f = operator.add
    g = operator.neg
    h = operator.getitem


print(C.f(1, 2), C().f(3, 4), C().g(5), C().h([7, 8], 1))
print(operator.sub(5, 2), operator.mul("ab", 2), operator.truediv(7, 2), operator.floordiv(7, 2), operator.mod(7, 3),
      operator.pow(2, 10), operator.lshift(1, 4), operator.rshift(16, 2), operator.and_(6, 3), operator.or_(4, 1),
      operator.xor(5, 1), operator.inv(5), operator.invert(0), operator.pos(-1), operator.abs(-2.5))
print(operator.lt(1, 2), operator.le(2, 2), operator.eq("a", "a"), operator.ne(1, 1), operator.gt(1, 2), operator.ge(3, 3))
print(operator.not_([]), operator.truth([0]), operator.is_(None, None), operator.is_not(1, None), operator.is_none(None),
      operator.is_not_none(0), operator.contains([1, 2], 2), operator.concat([1], [2]), operator.concat("a", "b"))
l = [1, 2, 3]
operator.setitem(l, 0, 9); operator.delitem(l, 1)
print(l, operator.iadd(l, [4]) is l, l, operator.iconcat(l, [5]), operator.imul([1], 2), operator.ipow(2, 3))
x = {1: "a"}
print(operator.getitem(x, 1), operator.index(7), operator.countOf([1, 1, 2], 1), operator.indexOf([4, 5], 5),
      operator.length_hint(iter(range(4))), operator.itemgetter(1)("ab"), operator.attrgetter("real")(3))
for f, a in [(operator.add, (1,)), (operator.add, (1, 2, 3)), (operator.neg, ()), (operator.concat, (1, 2)),
             (operator.setitem, ([],)), (operator.neg, ("a",)), (operator.delitem, ((1,), 0)), (operator.index, ())]:
    try:
        f(*a)
    except TypeError as e:
        print("TypeError", e)
for f in [operator.add, operator.neg, operator.setitem]:
    try:
        f(1, b=2)
    except TypeError as e:
        print("TypeError", e)
with tempfile.TemporaryDirectory() as d:
    os.makedirs(os.path.join(d, "a", "b"))
    for n in ["a/x.toml", "a/b/y.toml", "z.toml", "a/b/w.txt"]:
        open(os.path.join(d, n), "w").close()
    print(sorted(str(p.relative_to(d)) for p in pathlib.Path(d).glob("**/*.toml")))
    print(sorted(str(p.relative_to(d)) for p in pathlib.Path(d).rglob("*.txt")))
print(BaseExceptionGroup[ValueError], ExceptionGroup[int], BaseExceptionGroup[int].__origin__ is BaseExceptionGroup)


class G(ExceptionGroup[ValueError]):
    pass


print(G.__mro__[1].__name__, G("m", [ValueError(1)]).exceptions)

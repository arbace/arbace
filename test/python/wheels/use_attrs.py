# attrs (test/python/wheels.edn): classes by attrs.define and attr.s, validators, converters, evolve, asdict, ordering
import attr
import attrs


@attrs.define(order=True)
class Point:
    x: int = attrs.field(converter=int)
    y: int = attrs.field(default=0, validator=attrs.validators.instance_of(int))


@attr.s(frozen=True, slots=True)
class Tag:
    name = attr.ib()
    weight = attr.ib(default=1.0)


p = Point("3", 4)
print(p, repr(p), p == Point(3, 4), p < Point(3, 5))
q = attrs.evolve(p, y=9)
print(q, attrs.asdict(q), attrs.astuple(q))
print(sorted([Point(2, 1), Point(1, 2), Point(1, 1)]))
try:
    Point(1, "no")
except TypeError as e:
    print("TypeError", e.args[0])
t = Tag("a")
try:
    t.name = "b"
except attr.exceptions.FrozenInstanceError as e:
    print("frozen", type(e).__name__)
print(t, hash(t) == hash(Tag("a")), [f.name for f in attrs.fields(Tag)])
print(attrs.has(Point), attrs.has(int), attr.__version__ if hasattr(attr, "__version__") else "-")

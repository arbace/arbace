# six (test/python/wheels.edn): its Python 3 branch: moves, metaclasses, text helpers, iterators
import six
from six.moves import range, zip_longest, urllib


class Meta(type):
    def __new__(mcls, name, bases, ns):
        ns["tagged"] = name.lower()
        return super().__new__(mcls, name, bases, ns)


class Base(six.with_metaclass(Meta, object)):
    pass


@six.add_metaclass(Meta)
class Other(object):
    pass


print(six.PY2, six.PY3, six.string_types, six.integer_types, six.text_type, six.binary_type)
print(Base.tagged, Other.tagged, type(Other).__name__)
print(list(range(3)), list(zip_longest("ab", "c", fillvalue="-")))
print(six.b("abc"), six.u("abc"), six.ensure_str(b"x"), six.ensure_binary("y"), six.ensure_text(b"z"))
d = {"a": 1, "b": 2}
print(sorted(six.iteritems(d)), sorted(six.iterkeys(d)), sorted(six.itervalues(d)))
print(urllib.parse.quote("a b/c"), six.moves.reduce(lambda a, b: a * b, [1, 2, 3, 4]))
try:
    six.raise_from(ValueError("outer"), KeyError("inner"))
except ValueError as e:
    print(repr(e), repr(e.__cause__))
print(six.int2byte(65), six.byte2int(b"B"), six.indexbytes(b"abc", 1), list(six.iterbytes(b"hi")))

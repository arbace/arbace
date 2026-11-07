# queue 202: _sre's case-insensitive matching as sre_lib.h's (the simple case mappings of CPython's Unicode database,
# a set's lower case only, the locale's sets closed under case before NEGATE), and the match and pattern objects as
# sre.c's (a mutable subject read again by group(), match_getindex's __index__, pattern_repr's %.200R and flags)
import re
for p, s in [('K', 'K'), ('k', 'K'), ('K', 'K'), ('S', 'ſ'), ('ſ', 's'), ('В', 'ᲀ'),
             ('ᲀ', 'в'), ('ﬅ', 'ﬆ'), ('a[b-d]e', 'ABD'), ('a[^bc]d', 'ABD'), ('[^a-c]', 'B'),
             ('[^k]', 'K'), ('[a-z]', 'K'), ('İ', 'i'), ('i', 'İ'), ('\xdf', 'ẞ'),
             ('(.)\\1', 'aΑ'), ('(.)\\1', 'σς'), ('\\w+', '²Ⅷx'), ('\\d', '²'), ('\\d', '٣')]:
    print(ascii(p), ascii(s), [bool(re.search(p, s, f)) for f in (0, re.I, re.I | re.A)])
for p, s in [(b'[^bc]', b'B'), (b'[^Ax]', b'a'), (b'A', b'a'), (b'a', b'A'), (b'[a-c]', b'B'), (b'(.)\\1', b'aA'), (b'\\w', b'\xe9')]:
    print(p, s, [bool(re.search(p, s, f)) for f in (0, re.I, re.L, re.I | re.L)])
s = bytearray(b'abcdefgh')
m, m2 = re.search(b'[a-h]+', s), re.search(b'[e-h]+', s)
s[:] = b'xyz'
print(m.group(), m2.group(), m.span(), m2.span())
class Index:
    def __init__(self, v):
        self.v = v
    def __index__(self):
        return self.v
m = re.match('(a)(?P<n>b)', 'ab')
print(m.group(Index(1)), m[Index(2)], m.group('n'), m.group(Index(2), 1))
for g in (-1, 3, 1 << 1000, Index(1 << 1000), 'x', 1.0, Index(-1)):
    try:
        m.group(g)
    except (IndexError, TypeError) as e:
        print(type(e).__name__, e)
try:
    m.group([])
except Exception as e:
    print(type(e).__name__, e)
print(len(repr(re.compile('x' * 500))), repr(re.compile('Very ' + 'long ' * 100, re.I))[-40:])
print(repr(re.compile('a', re.U)), repr(re.compile('a', re.A | re.M)), repr(re.compile(b'a', re.L)), repr(re.compile('a', re.X | re.S)))

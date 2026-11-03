# Literals and their spellings (doc/python/SPEC.md §3): numbers, strings, bytes, prefixes, implicit
# concatenation, f-strings with conversions, specs and `=`, t-strings, the constant symbols.
from string.templatelib import Template, Interpolation

ints = [0, 7, 0x1F, 0o17, 0b101, 1_000, 00, 1234567890123456789012]
floats = [1.5, 1., .5, 1e3, 1_0.0_1E-3]
imags = [2j, 1.5J, 1e3j]
strs = ['a', "b", r"\d", u"x", """two
lines""", "say \"hi\"", 'a' "b" 'c']
blobs = [b'\x00', rb"\n", B"ab" b"cd"]
consts = (None, True, False, ...)
print(ints, floats, imags)
print(strs, blobs, consts)

x, w, y = 3.25, 8, "why"
print(f"a{{b}}{x!r:>{w}}{y = }\n", end="")
print(f"{x:{w}.{2}f}|{y!s}|{y!a}|" 'tail')
print(F'{x=!r:^10}' rf"\{y}")

name, n = "py", 41
t = t"hi {name!s:>10} { n + 1 }"
print(type(t) is Template, [type(p).__name__ for p in t], t.strings)
print([(i.value, i.expression, i.conversion, i.format_spec) for i in t.interpolations])

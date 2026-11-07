# PY6a: string.templatelib.Interpolation's __match_args__ (test_tstring's class patterns)
from string.templatelib import Interpolation
x = 42
t = t"a {x!r:>5} b {x + 1}"
print(Interpolation.__match_args__)
for i in t.interpolations:
    match i:
        case Interpolation(value, expression, conversion, format_spec):
            print(value, repr(expression), conversion, repr(format_spec))
match t.interpolations[0]:
    case Interpolation(42, "x"):
        print("matched")

# A built-in type's dict holds __doc__ (type_ready's type_dict_set_doc), so its instances' __doc__ is found: pyi's is
# None until the C docstrings come (PY5c's :docstrings), CPython's a str; email's _policybase reads every class
# attribute's __doc__ at import (queue 200, PY6-core)
for v in ("abc", 1, b"", [], {}, 1.5, (), {1}, range(1), None, True, 1j, bytearray()):
    d = v.__doc__
    print(type(v).__name__, "__doc__" in type(v).__dict__, d is None or isinstance(d, str))
import email.parser
print(email.parser.Parser.__name__)

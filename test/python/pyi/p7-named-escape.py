# PY7: \N{NAME} escapes the compiler's host does not know (aliases: \N{RS}, \N{NUL}, a correction alias) resolved by the
# runtime's Unicode data ({:joined} constants were "an unknown constant": test_configparser's subTests delimiters); a docstring of
# implicitly concatenated strs (it was none)
s = ' =:;#x\t\0\N{RS}\N{CEDILLA}\N{CAT}'
print([hex(ord(c)) for c in s])
print(ascii("\N{NUL}\N{LINE FEED}\N{BYTE ORDER MARK}\N{LATIN CAPITAL LETTER GHA}"))
print(ascii(f"a\N{RS}{1 + 1}\N{ZWNJ}b"), ascii("x" "\N{DEL}" 'y'))
def f():
    "doc\N{RS}"
print(ascii(f.__doc__))
class C:
    "a" "\N{RS}" 'b\N{SP}'
print(ascii(C.__doc__))
def g():
    "implicitly " 'concatenated'
    return 1
print(g.__doc__, g())

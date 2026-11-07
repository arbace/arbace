# PY6a: str() of a SyntaxError (SyntaxError_str: the message, the file's base name and the line), test_exceptions'
# test_string_source, testSyntaxErrorMissingParens and test_grammar's test_max_level
for args in [("m",), ("m", ("/a/b/f.py", 3, 1, "x", 3, 2)), ("m", (None, 3, 1, "x")), ("m", ("f.py", None, 1, "x")),
             (), (5,), ("m", ("f.py", "3", 1, "x"))]:
    print(repr(str(SyntaxError(*args))))
try:
    compile('return "x"', "<string>", "exec")
except SyntaxError as e:
    print(str(e))
try:
    compile("(" * 201 + ")" * 201, "<string>", "eval")
except SyntaxError as e:
    print(str(e))
e = SyntaxError("m", ("f.py", 1, 1, "x"))
e.msg = "changed"
print(str(e), e.args)

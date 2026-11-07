# PY6-core: SyntaxError_init's checks of its details and the concatenations of a Template with a str
# (test_exceptions' test_incorrect_constructor, test_syntax_error_memory_leak; test_tstring's test_template_concatenation)
# and the sequences' concatenation messages (PyNumber_Add's sq_concat fallback)
for a in [("bad.py", 1, 2), ("bad.py", 1, 2, 4, 5, 6, 7, 8), ("bad.py", 1, 2, "abcdefg", 1), 5, "abcd", [1, 2, 3, 4],
          ("f.py", 1, 2, "t", 1, 3, None)]:
    try:
        e = SyntaxError("m", a)
        print("ok", e.args, e.filename, e.lineno, e.end_offset)
    except TypeError as x:
        print("TypeError", x)
e = SyntaxError("msg", ("file.py", 1, 2, "txt", 2, 3))
e.__init__("new_msg", ("new_file.py", 2, 3, "new_txt", 3, 4))
print(e.msg, e.args, e.filename, e.lineno, e.offset, e.text, e.end_lineno, e.end_offset)
print(SyntaxError("a", "b", "c").msg, SyntaxError().msg, SyntaxError("x").filename)
for f in (lambda: "Hello, " + t"{1}", lambda: t"{1}" + "x", lambda: t"{1}" + 5, lambda: 5 + t"{1}"):
    try:
        f()
    except TypeError as x:
        print("TypeError", str(x).replace("'Template'", "'string.templatelib.Template'"))
print((t"a" + t"b").strings)
for a, b in [("a", 1), ([1], 1), ((1,), 1), (b"a", 1), (bytearray(b"a"), 1), ("a", None)]:
    try:
        a + b
    except TypeError as e:
        print(e)

# queue 232: ast.parse of a str constant holding a lone surrogate keeps it (the native reader's JSON answer: its UTF-16
# units, which Go's JSON decoder would have made U+FFFD; pytest's rewritten modules, click's "\udce2" tests); print()'s
# writes to a file object one by one, as builtin_print_impl gives them
import ast
src = 'x = "\\udce2"\ny = b"\\xff"\n'
t = ast.parse(src)
print(ascii(t.body[0].value.value), ascii(ast.unparse(t)))
ns = {}
exec(compile(t, "<s>", "exec"), ns)
print(ascii(ns["x"]), ns["y"])
t2 = ast.parse(src.encode())
print(ascii(t2.body[0].value.value))
ns2 = {}
exec(compile(src.encode(), "<s>", "exec"), ns2)
print(ascii(ns2["x"]))
print(ascii(ast.literal_eval('"\\ud800x\\udfff"')), ascii(ast.parse('f"{1}\\udce2"').body[0].value.values[1].value))


class W:
    def __init__(self):
        self.calls = []

    def write(self, s):
        self.calls.append(s)


w = W()
print("a", 1, None, file=w)
print(file=w)
print("x", "y", sep="", end="", file=w)
print(w.calls)

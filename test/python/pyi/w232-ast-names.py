# queue 232: compile() of an AST whose identifiers are no Python identifiers (pytest's assertion rewriting names its
# temporaries @py_assert1, @py_builtins): CPython compiles the names as given; pyi unparses the tree, each such name
# respelled consistently, the caller's tree unchanged
import ast

t = ast.parse("import builtins as x\nprint(x.len('ab'))\ndef f(y):\n    z = y + 1\n    del z\n    return y\nprint(f(2))\n"
              "global g\ng = 5\nprint(g)\n")
t.body[0].names[0].asname = "@py_builtins"
t.body[1].value.args[0].func.value.id = "@py_builtins"
fn = t.body[2]
fn.args.args[0].arg = "@a"
fn.body[0].targets[0].id = "@py_assert1"
fn.body[0].value.left.id = "@a"
fn.body[1].targets[0].id = "@py_assert1"
fn.body[2].value.id = "@a"
t.body[4].names[0] = "my-g"
t.body[5].targets[0].id = "my-g"
t.body[6].value.args[0].id = "my-g"
ns = {}
exec(compile(t, "<t>", "exec"), ns)
print(t.body[0].names[0].asname, fn.args.args[0].arg, t.body[4].names)
t2 = ast.parse("x = 1")
t2.body[0].targets[0].id = "1x"
exec(compile(t2, "<t2>", "exec"), {})
print("compiled", ast.dump(t2.body[0].targets[0]))
t3 = ast.parse("import os.path as p\nfrom os import sep as s\nprint(p.join('a', 'b'), s)\n")
t3.body[0].names[0].asname = "@pytest_ar"
t3.body[1].names[0].asname = "@s"
t3.body[2].value.args[0].func.value.id = "@pytest_ar"
t3.body[2].value.args[1].id = "@s"
exec(compile(t3, "<t3>", "exec"), {})

# queue 202c: _ast's object model as CPython 3.14's: compile() of an AST object converted (obj2ast) and validated
# (Python/ast.c), the node constructors' rules and deprecation warnings, __replace__, __reduce__, and
# ast.parse(optimize=...)'s preprocessing (Python/ast_preprocess.c).
import ast, copy, pickle, warnings

def err(f):
    try:
        f()
    except Exception as e:
        return type(e).__name__ + ": " + str(e)
    return "ok"

def mod(m, mode="exec"):
    m.lineno = m.col_offset = 0
    ast.fix_missing_locations(m)
    return compile(m, "<test>", mode)

def expr(e):
    return err(lambda: mod(ast.Module([ast.Expr(e)], [])))

def stmt(s):
    return err(lambda: mod(ast.Module([s], [])))

# the validator
print(expr(ast.Name("x", ast.Store())))
print(expr(ast.BoolOp(ast.And(), [])))
print(expr(ast.BoolOp(ast.And(), [ast.Constant(1), None])))
print(expr(ast.Dict([ast.Constant(1)], [])))
print(expr(ast.Compare(ast.Name("x", ast.Load()), [ast.Eq()], [])))
print(expr(ast.Constant([1])))
print(expr(ast.Constant((1, frozenset({2}), b"x"))))
print(expr(ast.ListComp(ast.Name("x", ast.Load()), [])))
print(expr(ast.Name("True", ast.Load())))
print(expr(ast.NamedExpr(ast.Constant(1), ast.Constant(2))))
print(stmt(ast.FunctionDef("x", ast.arguments(), [], [], None, None, [])))
print(stmt(ast.Assign([], ast.Constant(3))))
print(stmt(ast.Delete([ast.Name("x", ast.Load())])))
print(stmt(ast.Raise(None, ast.Constant(1))))
print(stmt(ast.Try([ast.Pass()], [], [], [])))
print(stmt(ast.Try([ast.Pass()], [], [ast.Pass()], [ast.Pass()])))
print(stmt(ast.ImportFrom("m", [ast.alias("x")], -1)))
print(stmt(ast.Global([])))
print(stmt(ast.AnnAssign(ast.Attribute(ast.Name("a", ast.Load()), "b", ast.Store()), ast.Name("int", ast.Load()), None, 1)))
print(stmt(ast.With([], [ast.Pass()])))
print(stmt(ast.Match(ast.Name("x", ast.Load()), [ast.match_case(ast.MatchStar("y"), None, [ast.Pass()])])))
print(stmt(ast.Match(ast.Name("x", ast.Load()), [ast.match_case(ast.MatchAs(None, "_"), None, [ast.Pass()])])))
print(stmt(ast.Match(ast.Name("x", ast.Load()), [ast.match_case(ast.MatchValue(ast.Constant(None)), None, [ast.Pass()])])))
# obj2ast
print(err(lambda: compile(ast.Module([ast.Expr(ast.expr(lineno=2, col_offset=3), lineno=2, col_offset=3)], []), "<t>", "exec")))
print(err(lambda: compile(ast.Module([ast.Expr(ast.Name(42, ast.Load(), lineno=1, col_offset=0), lineno=1, col_offset=0)], []), "<t>", "exec")))
print(err(lambda: compile(ast.Module([ast.Expr(ast.Constant(1))], []), "<t>", "exec")))
print(err(lambda: compile(ast.Module(ast.Pass(), []), "<t>", "exec")))
print(err(lambda: compile(ast.Expression(ast.Constant(1, lineno=1, col_offset=0)), "<t>", "exec")))
print(err(lambda: compile(ast.Module([ast.Expr(ast.YieldFrom(None, lineno=1, col_offset=0), lineno=1, col_offset=0)], []), "<t>", "exec")))
print(err(lambda: compile(ast.Module([ast.ImportFrom("m", [ast.alias("x", lineno=1, col_offset=0)], "1", lineno=1, col_offset=0)], []), "<t>", "exec")))
print(err(lambda: compile(ast.Module([ast.Pass(lineno=2, col_offset=0, end_lineno=1, end_col_offset=0)], []), "<t>", "exec")))
print(err(lambda: compile(ast.Module([ast.Pass(lineno=1, col_offset=5, end_lineno=1, end_col_offset=2)], []), "<t>", "exec")))
t = ast.parse("x = 1")
c = compile(t, "<t>", "exec", ast.PyCF_ONLY_AST)
print(c is t, ast.dump(c) == ast.dump(t))
ns = {}
exec(compile(t, "<t>", "exec"), ns)
print(ns["x"])
# the constructors
warnings.simplefilter("always")
def warns(f):
    with warnings.catch_warnings(record=True) as w:
        warnings.simplefilter("always")
        r = f()
    return [str(x.message) for x in w], r
print(warns(lambda: ast.FunctionDef(args=ast.arguments()))[0])
print(warns(lambda: ast.Name())[0])
print(ast.Name("x").ctx.__class__.__name__)
print(warns(lambda: ast.Constant(1, foo='bar'))[0])
print(sorted(warns(lambda: ast.BinOp())[0]))
print(warns(lambda: ast.Constant())[0])
class AllTypes(ast.AST):
    _fields = ('a', 'b')
    _field_types = {'a': int | None, 'b': list[str]}
    a: int | None = None
print(AllTypes().b, AllTypes().a)
class Incomplete(ast.AST):
    _fields = ('a', 'b')
    _field_types = {'a': int | None}
print(warns(lambda: Incomplete())[0])
print(err(lambda: ast.Constant(1, value=2)))
# __replace__ and __reduce__
n = ast.Name("x", ast.Load(), lineno=1, col_offset=0)
r = copy.replace(n, id="y")
print(ast.dump(r, include_attributes=True), r.ctx is n.ctx)
print(err(lambda: copy.replace(n, unknown=1)))
print(err(lambda: copy.replace(ast.Name(ctx=ast.Load()) if False else ast.Name.__new__(ast.Name), ctx=ast.Load())))
print(err(lambda: n.__replace__(1)))
print(n.__reduce__()[1], ast.dump(copy.deepcopy(ast.parse("f(a, *b)"))))
print(ast.dump(pickle.loads(pickle.dumps(ast.parse("x[1:2] = 3")))))
# optimize
print(ast.dump(ast.parse("'%s %-5r %%' % (a, b)", optimize=1)))
print(ast.dump(ast.parse("'%s %d' % (a, b)", optimize=1)))
print(ast.dump(ast.parse("x = __debug__", optimize=1)))
print(ast.dump(ast.parse("x = __debug__", optimize=0)))
print(ast.dump(ast.parse("match x:\n case -1 | 1+2j | {-0.5: 1}: pass", optimize=1)))
print(ast.dump(ast.parse("def f():\n  'doc'\nclass C:\n  'doc'\n  x = 1\n", optimize=2), include_attributes=True))
print(ast.dump(ast.parse("'doc'\n'second'\n", optimize=2)))
print(ast.dump(compile(ast.parse("y = __debug__"), "<t>", "exec", ast.PyCF_OPTIMIZED_AST, optimize=1)))
# the classes' signatures, repr, binding, interned identifiers
print(ast.withitem.__doc__, ast.GtE.__doc__, ast.Name.__doc__, ast.arguments.__doc__, sep="\n")
print(ast.cmpop.__doc__)
print(ast.mod.__doc__)
print(repr(ast.parse("def f(): pass")))
print(repr(ast.parse("[a, b, c, (d, e, f)]", mode="eval")))
class N(ast.Constant):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.z = "spam"
n = N(42)
print(n.value, n.z, repr(n), n.__repr__())
t = ast.parse("x.y = f(a, k=1)\nimport m as n\nglobal g")
import sys
print(t.body[0].targets[0].value.id is sys.intern("x"), t.body[0].targets[0].attr is sys.intern("y"),
      t.body[1].names[0].asname is sys.intern("n"), t.body[2].names[0] is sys.intern("g"))
# AST's own tp_name, "ast.AST"
print(repr(ast.AST()))
saved = ast.AST._fields
del ast.AST._fields
print(err(lambda: ast.AST()), err(lambda: ast.AST(arg1=123)))
ast.AST._fields = saved

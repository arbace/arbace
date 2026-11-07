# PY6a: _ast's node classes (ast's names, fields, defaults, repr, match, dump), against CPython
import ast
n = ast.Name('x', ast.Load())
print(repr(n), n.id, ast.Name._fields, ast.expr._attributes, ast.Name.__mro__, isinstance(n, ast.expr))
print(repr(ast.BinOp(ast.Constant(1), ast.Add(), ast.Constant(2))), ast.Module([], []).body, repr(ast.arguments()))
f = ast.FunctionDef('f', ast.arguments(), [ast.Pass()], [])
print(f.returns, f.type_params, ast.Pass().__match_args__, ast.Load() is not ast.Load())
match n:
    case ast.Name(id=i):
        print('matched', i)
print(ast.dump(ast.BinOp(ast.Constant(1), ast.Add(), ast.Constant(2))))

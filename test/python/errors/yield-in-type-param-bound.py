# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): yield-in-type-param-bound
def f[T: (yield)](): pass

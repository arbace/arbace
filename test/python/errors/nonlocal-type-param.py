# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): nonlocal-type-param
def f[T]():
    def g():
        nonlocal T

# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): yield-in-listcomp
def f():
    return [(yield x) for x in y]

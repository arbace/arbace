# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): nonlocal-after-assign
def g():
    x = 1
    def f():
        x = 2
        nonlocal x

# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): nonlocal-and-global
def g():
    x = 1
    def f():
        global x
        nonlocal x

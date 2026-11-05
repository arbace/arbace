# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): nonlocal-param
def g():
    x = 1
    def f(x):
        nonlocal x

# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): nonlocal-after-use
def g():
    x = 1
    def f():
        print(x)
        nonlocal x

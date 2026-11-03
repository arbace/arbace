# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): global-after-assign
def f():
    x = 1
    global x

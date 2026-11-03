# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): global-after-use
def f():
    print(x)
    global x

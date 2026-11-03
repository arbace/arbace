# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): global-annot
def f():
    x: int
    global x

# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): annotated-global
def f():
    global x
    x: int = 1

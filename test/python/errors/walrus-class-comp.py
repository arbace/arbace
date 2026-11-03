# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): walrus-class-comp
class C:
    [(y := x) for x in z]

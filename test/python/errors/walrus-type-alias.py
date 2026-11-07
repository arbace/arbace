# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): walrus-type-alias
type A = [(y := 1) for x in z]

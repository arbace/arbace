# A program CPython's symbol table rejects (bin/pyi scope-check, PY5a): async-comprehension-outside
def f():
    return [x async for x in y]

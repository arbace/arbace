# PY6a: the scope analysis' SyntaxErrors located (symtable.c's SET_ERROR_LOCATION): test_grammar's test_yield,
# test_yield_in_comprehensions and test_var_annot_syntax_errors
srcs = ["def g(): yield from (), 1", "def g(): 1, yield 1", "def g(): f(yield 1)", "yield", "yield from", "class foo:yield 1",
        "def g(a:(yield)): pass", "def g(): [(yield x) for x in ()]", "[(yield x) for x in ()]", "class C: [(yield x) for x in ()]",
        "def f: int", "x: int: str", "def f():\n    nonlocal x: int\n", "x: int = y = 1", "[x, 0]: int\n", "f(): int\n", "(x,): int",
        "def f():\n    (x, y): int = (1, 2)\n", "def f():\n    x: int\n    global x\n", "def f():\n    global x\n    x: int\n",
        "def f():\n    x: int\n    nonlocal x\n", "def f():\n    nonlocal x\n    x: int\n"]
for s in srcs:
    try:
        compile(s, "<test string>", "exec")
        print(repr(s), "compiled")
    except SyntaxError as e:
        print(repr(s), e.msg, e.lineno, e.offset)

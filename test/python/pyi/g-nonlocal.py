# PY6-core: the scope analysis' second-pass SyntaxErrors at their directive's location (symtable.c's error_at_directive):
# test_type_params' test_nonlocal_disallowed_02/03, and nonlocal's and global's other errors
for s in ["def outer2[T]():\n    def inner1():\n        nonlocal T\n", "class Cls[T]:\n    nonlocal T\n",
          "def f():\n    nonlocal x\n", "nonlocal x\n", "def f():\n    x = 1\n    def g():\n        global x\n        nonlocal x\n",
          "def f[T]():\n    T = 1\n    def g():\n        nonlocal T\n        T = 2\n"]:
    try:
        compile(s, "<s>", "exec")
        print("ok")
    except SyntaxError as e:
        print(e.msg, e.lineno, e.offset)

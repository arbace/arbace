# PY6-core: the compiler's checks of patterns (test_patma's TestSyntaxErrors): duplicate mapping keys by value (0, 0.0,
# 0j), and f-strings neither as value patterns nor as mapping keys
for src in ["match ...:\n case {0: _, 0.0: _}:\n  pass\n", "match ...:\n case {0: _, 0j: _}:\n  pass\n",
            "match ...:\n case {1.5: _, 1.5: _}:\n  pass\n", "match ...:\n case {0: _, 0.5: _, 1j: _}:\n  pass\n",
            "match ...:\n case {f'': _}:\n  pass\n", "match ...:\n case f'':\n  pass\n", "match ...:\n case f'{x}':\n  pass\n",
            "match ...:\n case {'a': _, 'a': _}:\n  pass\n", "match ...:\n case 'a' | 'b':\n  pass\n"]:
    try:
        compile(src, "<s>", "exec")
        print("ok")
    except SyntaxError as e:
        print("SyntaxError", e.msg, e.lineno)

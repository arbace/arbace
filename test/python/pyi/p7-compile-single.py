# PY7: compile(..., "single") applies codeop's incomplete-input rule only with PyCF_ALLOW_INCOMPLETE_INPUT (doctest
# compiles each example so: a compound statement was "incomplete input", test_syntax's doctest)
import codeop, doctest
src = "def f():\n    return 42\n"
exec(compile(src, "<s>", "single"))
print(f())
exec(compile("for i in range(2):\n    print(i)\n", "<s>", "single"))
print(codeop.compile_command("if True:") is None, codeop.compile_command("x = 1") is not None)
try:
    compile("if True:\n", "<s>", "single", 0x4000)
except SyntaxError as e:
    print("SyntaxError", e.msg)
def g():
    """
    >>> def h(x):
    ...     return x + 1
    >>> h(2)
    3
    >>> for c in "ab":
    ...     print(c)
    a
    b
    """
print(doctest.run_docstring_examples(g, {}, verbose=False))

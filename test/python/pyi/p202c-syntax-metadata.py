# queue 202c: SyntaxError._metadata (the parser's last statement's location and the source,
# _PyPegen_set_syntax_error_metadata) and traceback's keyword typo suggestions from it ("Did you mean ...?").
import traceback

cases = [
    "with block ad something:\n  pass\n",
    "fur a in b:\n  pass\n",
    "for a in b:\n  pass\nelso:\n  pass\n",
    "x = 1\ny = 2\nwhille True:\n  pass\n",
    "def f():\n  retrun 1\n",
    "impotr os\n",
    "a = 1 +\n",
    "if x:\npass\n",
    "x = (1,\n",
    "f(a b)\n",
]
for src in cases:
    try:
        compile(src, "<case>", "exec")
    except SyntaxError as e:
        print(repr(e.msg), e._metadata)
        print("".join(traceback.format_exception_only(e)).rstrip().splitlines()[-1])
import ast
try:
    compile("for a in b:", "<input>", "exec", ast.PyCF_ONLY_AST | 0x4000 | 0x200)
except SyntaxError as e:
    print(type(e).__name__, e.args, e._metadata)
print(SyntaxError("m")._metadata if hasattr(SyntaxError("m"), "_metadata") else "none")
# _suggestions (Python/suggestions.c): the first suggestion traceback takes, before difflib's
import _suggestions, keyword
for w in ["of", "fur", "whille", "elso", "impotr", "Tru", "lamda", "x", "async_", "NONE"]:
    print(w, _suggestions._generate_suggestions(keyword.kwlist, w))
print(_suggestions._generate_suggestions(["apple", "Apple", "apply"], "appel"))
try:
    compile("[\nx for x\nin range(3)\nof x\n]\n", "<case>", "exec")
except SyntaxError as e:
    print("".join(traceback.format_exception_only(e)).rstrip().splitlines()[-1])

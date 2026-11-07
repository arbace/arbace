"""The syntax-error cases of repr/python's tree check (bin/repr-python parse-check syntax-errors; doc/python/PYTHON-SURVEY.md,
"As built: PY9 (errors)"): the sources CPython's own tests reject, taken from the pinned tarball's Lib/test, one file each.

    syntax_cases.py --src PYTHON-3.14.10-DIR --out DIR

- Lib/test/test_syntax.py's doctests: each example (its >>> and ... lines) whose expected output is a SyntaxError,
  IndentationError or TabError traceback;
- the string constants given as the first argument of a call of _check_error, check, assertSyntaxError and the like
  (assertAllRaise's third, a list of them), in test_syntax.py, test_exceptions.py (testSyntaxErrorOffset's cases and the rest), test_fstring.py,
  test_tstring.py, test_grammar.py, test_patma.py, test_type_params.py and test_positional_only_arg.py.

Every file is read as data (ast.parse of the tests' own text only finds the constants; nothing is imported or run). A
case is written as DIR/<test>-NNNN.py (UTF-8), the list of names on stdout. The files are the reference's input: the
helper's ast.parse decides what each is, an accepted case being judged as a tree like any other file."""

import ast
import os
import re
import sys

CALLS = {"_check_error", "check", "assertSyntaxError", "_check_syntax_error", "check_syntax_error",
         "assertInvalidSyntax", "assertAllRaise", "_check_in_scopes"}
FILES = ["test_syntax.py", "test_exceptions.py", "test_fstring.py", "test_tstring.py", "test_grammar.py",
         "test_patma.py", "test_type_params.py", "test_positional_only_arg.py", "test_unpack_ex.py", "test_genexps.py"]
DOCTEST_ERR = re.compile(r"^(SyntaxError|IndentationError|TabError)\b")


def doctest_cases(text):
    """The examples of the module's docstrings whose expected output ends in a syntax error"""
    out = []
    lines = text.split("\n")
    i = 0
    while i < len(lines):
        m = re.match(r"^(\s*)>>> ?(.*)$", lines[i])
        if not m:
            i += 1
            continue
        ind = m.group(1)
        src = [m.group(2)]
        i += 1
        while i < len(lines) and re.match(r"^" + re.escape(ind) + r"\.\.\.(?: |$)", lines[i]):
            src.append(lines[i][len(ind) + 4:])
            i += 1
        expect = []
        while i < len(lines) and lines[i].strip() and not re.match(r"^\s*>>>", lines[i]):
            expect.append(lines[i].strip())
            i += 1
        if any(DOCTEST_ERR.match(e) for e in expect):
            out.append("\n".join(src) + "\n")
    return out


def call_cases(tree):
    """The string constants given first to the error-checking calls"""
    out = []
    for n in ast.walk(tree):
        if isinstance(n, ast.Call) and n.args:
            f = n.func
            name = f.attr if isinstance(f, ast.Attribute) else (f.id if isinstance(f, ast.Name) else None)
            # assertAllRaise(EXC, MSG, [SOURCES...]): the sources are the third argument
            a = n.args[2] if name == "assertAllRaise" and len(n.args) > 2 else n.args[0]
            if name in CALLS and isinstance(a, ast.Constant) and isinstance(a.value, (str, bytes)):
                out.append(a.value)
            elif name in CALLS and isinstance(a, (ast.List, ast.Tuple)):
                for e in a.elts:
                    if isinstance(e, ast.Constant) and isinstance(e.value, (str, bytes)):
                        out.append(e.value)
    return out


def main(argv):
    src = argv[argv.index("--src") + 1]
    out = argv[argv.index("--out") + 1]
    os.makedirs(out, exist_ok=True)
    names = []
    for f in FILES:
        p = os.path.join(src, "Lib", "test", f)
        if not os.path.exists(p):
            continue
        text = open(p, encoding="utf-8").read()
        cases = []
        if f == "test_syntax.py":
            cases += doctest_cases(text)
        cases += call_cases(ast.parse(text))
        seen = set()
        k = 0
        for c in cases:
            data = c if isinstance(c, bytes) else c.encode("utf-8", "surrogatepass")
            if data in seen:
                continue
            seen.add(data)
            k += 1
            name = "%s-%04d.py" % (f[:-3], k)
            with open(os.path.join(out, name), "wb") as w:
                w.write(data)
            names.append(name)
    sys.stdout.write("".join(n + "\n" for n in sorted(names)))


if __name__ == "__main__":
    main(sys.argv)

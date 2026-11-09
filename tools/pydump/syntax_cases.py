"""The syntax-error cases of repr/python's tree check (bin/repr-python parse-check syntax-errors; doc/python/PYTHON-SURVEY.md,
"As built: PY9 (errors)"): the sources CPython's own tests reject, taken from the pinned tarball's Lib/test, one file each.

    syntax_cases.py --src PYTHON-3.14.10-DIR --out DIR [--expect FILE]

- Lib/test/test_syntax.py's doctests: each example (its >>> and ... lines) whose expected output is a SyntaxError,
  IndentationError or TabError traceback;
- the string constants given as the first argument of a call of _check_error, check, assertSyntaxError and the like
  (assertAllRaise's third, a list of them), in test_syntax.py, test_exceptions.py (testSyntaxErrorOffset's cases and the rest), test_fstring.py,
  test_tstring.py, test_grammar.py, test_patma.py, test_type_params.py and test_positional_only_arg.py.

Every file is read as data (ast.parse of the tests' own text only finds the constants; nothing is imported or run). A
case is written as DIR/<test>-NNNN.py (UTF-8), the list of names on stdout. The files are the reference's input: the
helper's ast.parse decides what each is, an accepted case being judged as a tree like any other file.

--expect FILE (queue 240: each case's own verdict, the error its test expects): one map a line, read with the Clojure
reader, for every case whose test states the error in a way this script knows (a case may have several, all its tests'):
  {:case NAME :helper H :mode M :kind K :exact MSG :ellipsis B :ignore-detail B}   a doctest: the example's expected
      exception line (doctest compares format_exception_only's last line, "Kind: message", in mode "single"); :ellipsis
      and :ignore-detail its +ELLIPSIS and +IGNORE_EXCEPTION_DETAIL
  {:case NAME :helper H :mode M :search RE :filename F :subclass K :lineno L :offset O :end-lineno L :end-offset O}
      test_syntax's _check_error (RE searched in str(err), "message (F, line L)"; :subclass IndentationError or
      TabError when given), test_exceptions' check (the positions alone), test_fstring's and test_tstring's
      assertAllRaise(SyntaxError, RE, [...]) (mode "eval", F "<string>")
A case with no line is judged by its record alone (the helper's tree). The script runs on pyi too (bin/pyi script: no
python3 at check time), its output byte for byte the same."""

import ast
import json
import os
import re
import sys

CALLS = {"_check_error", "check", "assertSyntaxError", "_check_syntax_error", "check_syntax_error",
         "assertInvalidSyntax", "assertAllRaise", "_check_in_scopes"}
FILES = ["test_syntax.py", "test_exceptions.py", "test_fstring.py", "test_tstring.py", "test_grammar.py",
         "test_patma.py", "test_type_params.py", "test_positional_only_arg.py", "test_unpack_ex.py", "test_genexps.py"]
DOCTEST_ERR = re.compile(r"^(SyntaxError|IndentationError|TabError)\b")


DOCTEST_FLAG = re.compile(r"#\s*doctest:\s*([^\n]*)")
EXC_HDR = re.compile(r"^Traceback \((?:most recent call last|innermost last)\):\s*$")


def doctest_expect(src, expect):
    """The expectation of a doctest example (doctest's _EXCEPTION_RE: the header, then the stack, then the message from
    the first line that starts with a word character), or None"""
    # the docstring's end is not the example's output (the file's lines, not the docstring's text, are read here)
    for i, e in enumerate(expect):
        if '"""' in e:
            expect = expect[:i] + ([e[:e.index('"""')].rstrip()] if e[:e.index('"""')].strip() else [])
            break
    if not expect or not EXC_HDR.match(expect[0]):
        return None
    msg = None
    for i in range(1, len(expect)):
        if re.match(r"\w", expect[i]):
            msg = expect[i:]
            break
    if msg is None or len(msg) != 1:
        return None
    m = re.match(r"^(SyntaxError|IndentationError|TabError): (.*)$", msg[0])
    if not m:
        return None
    flags = " ".join(DOCTEST_FLAG.findall(src))
    return {"helper": "doctest", "mode": "single", "kind": m.group(1), "exact": m.group(2),
            "ellipsis": "+ELLIPSIS" in flags, "ignore-detail": "+IGNORE_EXCEPTION_DETAIL" in flags}


def doctest_cases(text):
    """The examples of the module's docstrings whose expected output ends in a syntax error, each with its expectation"""
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
            code = "\n".join(src) + "\n"
            out.append((code, doctest_expect(code, expect)))
    return out


def const(n, kinds=(str, bytes, int)):
    """A constant argument's value, or the reason it is none"""
    if isinstance(n, ast.Constant) and isinstance(n.value, kinds) and not isinstance(n.value, bool):
        return n.value
    raise ValueError("not a constant")


def call_expect(f, name, n):
    """The expectation the call states of its source (a helper this script knows), or None"""
    kw = {k.arg: k.value for k in n.keywords if k.arg}
    try:
        if f == "test_syntax.py" and name == "_check_error":
            pos = ["code", "errtext", "filename", "mode", "subclass", "lineno", "offset", "end_lineno", "end_offset"]
            a = dict(zip(pos, n.args))
            a.update(kw)
            if "errtext" not in a:
                return None
            e = {"helper": "_check_error", "search": const(a["errtext"], (str,)),
                 "filename": const(a["filename"], (str,)) if "filename" in a else "<testcase>",
                 "mode": const(a["mode"], (str,)) if "mode" in a else "exec"}
            if "subclass" in a:
                if not isinstance(a["subclass"], ast.Name):
                    return None
                e["subclass"] = a["subclass"].id
            for k in ["lineno", "offset", "end_lineno", "end_offset"]:
                if k in a and not (isinstance(a[k], ast.Constant) and a[k].value is None):
                    e[k.replace("_", "-")] = const(a[k], (int,))
            return e
        if f == "test_exceptions.py" and name == "check" and len(n.args) >= 3:
            pos = ["src", "lineno", "offset", "end_lineno", "end_offset", "encoding"]
            a = dict(zip(pos, n.args))
            a.update(kw)
            e = {"helper": "check", "mode": "exec", "filename": "<fragment>"}
            for k in ["lineno", "offset", "end_lineno", "end_offset"]:
                if k in a:
                    e[k.replace("_", "-")] = const(a[k], (int,))
            return e
        if name == "assertAllRaise" and len(n.args) > 2 and isinstance(n.args[0], ast.Name) and n.args[0].id == "SyntaxError":
            return {"helper": "assertAllRaise", "mode": "eval", "filename": "<string>", "search": const(n.args[1], (str,))}
    except ValueError:
        return None
    return None


def call_cases(tree, path=""):
    """The string constants given first to the error-checking calls, each with its expectation"""
    out = []
    for n in ast.walk(tree):
        if isinstance(n, ast.Call) and n.args:
            f = n.func
            name = f.attr if isinstance(f, ast.Attribute) else (f.id if isinstance(f, ast.Name) else None)
            # assertAllRaise(EXC, MSG, [SOURCES...]): the sources are the third argument
            a = n.args[2] if name == "assertAllRaise" and len(n.args) > 2 else n.args[0]
            if name in CALLS and isinstance(a, ast.Constant) and isinstance(a.value, (str, bytes)):
                out.append((a.value, call_expect(path, name, n)))
            elif name in CALLS and isinstance(a, (ast.List, ast.Tuple)):
                x = call_expect(path, name, n) if name in CALLS else None
                for e in a.elts:
                    if isinstance(e, ast.Constant) and isinstance(e.value, (str, bytes)):
                        out.append((e.value, x))
    return out


def edn(v):
    """A value as the Clojure reader reads it (JSON's strings are Clojure's; True and False are true and false)"""
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, int):
        return str(v)
    return json.dumps(v)


def main(argv):
    src = argv[argv.index("--src") + 1]
    out = argv[argv.index("--out") + 1]
    expect = argv[argv.index("--expect") + 1] if "--expect" in argv else None
    os.makedirs(out, exist_ok=True)
    names = []
    expects = []
    for f in FILES:
        p = os.path.join(src, "Lib", "test", f)
        if not os.path.exists(p):
            continue
        text = open(p, encoding="utf-8").read()
        cases = []
        if f == "test_syntax.py":
            cases += doctest_cases(text)
        cases += call_cases(ast.parse(text), f)
        seen = {}
        k = 0
        for c, x in cases:
            data = c if isinstance(c, bytes) else c.encode("utf-8", "surrogatepass")
            if data in seen:
                name = seen[data]
            else:
                k += 1
                name = "%s-%04d.py" % (f[:-3], k)
                seen[data] = name
                with open(os.path.join(out, name), "wb") as w:
                    w.write(data)
                names.append(name)
            if x is not None:
                expects.append("{:case %s %s}" % (edn(name), " ".join(":%s %s" % (key, edn(x[key])) for key in sorted(x))))
    sys.stdout.write("".join(n + "\n" for n in sorted(names)))
    if expect:
        # sorted by case, a case's lines in its tests' order; the same line twice once
        by = {}
        for e in expects:
            by.setdefault(e.split('"')[1], [])
            if e not in by[e.split('"')[1]]:
                by[e.split('"')[1]].append(e)
        with open(expect, "w", encoding="utf-8") as w:
            w.write("".join(e + "\n" for n in sorted(by) for e in by[n]))


if __name__ == "__main__":
    main(sys.argv)

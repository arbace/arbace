#!/usr/bin/env python3
"""repr/python's helper (doc/python/SPEC.md; PYS3 (a)): CPython's own parser, run as `python3 -I -S -B`, writing
Python files as Python forms in mode :lines, and the facts the round trip judges by.

usage:
  pydump.py tree --base DIR --list FILE --out DIR [--tsv FILE] [--full]
      every file of the list (paths relative to DIR, one a line) parsed by ast.parse (the read level's parse) and
      written as OUT/REL.ae: (python/module [...] {...}) in the spec's forms (the literals' spellings from the
      source, §3), or (python/error ...) when ast.parse rejects it (§12). Type comments come from a second parse
      with type_comments=True (§5.8, S6): a file only it rejects gets {:type-comments :rejected}. A Name, Attribute,
      Subscript, Starred, List or Tuple in a Store or Del context is marked (^:store, ^:del on a symbol,
      {:ctx :store} on a list) for the converter, which checks the mark against the slot and drops it (S15).
      With --full, mode :full (§11, PY3): every node with attributes in its long spelling with :pos, and the
      module's map with :mode :full, :tokens (the token stream, the text between tokens as :ws: the texts
      concatenated are the decoded file), :encoding (when not UTF-8) and :bom.
      The TSV, one line a file: REL STATUS AST CODE MARSHAL SHA NODES SECONDS RSS
        STATUS  ok | error
        AST     sha256 of ast.dump(tree) (no attributes, the plain parse): the round trip's level 2
        CODE    sha256 of the code objects compiled from the text, normalized without positions (code_digest):
                the round trip's level 3; "compile-error: MSG" when compile rejects what ast.parse accepted
        MARSHAL sha256 of marshal.dumps of the code compiled from the text, positions included (level 5)
        SHA     sha256 of the file's bytes (level 4: the :full print against the original)
        NODES   the tree's nodes; SECONDS the file's time; RSS the process's peak RSS so far (KiB, ru_maxrss)
  pydump.py scopes --base DIR --list FILE --out DIR
                                 each file's symbol table as CPython's _symtable builds it, the raw flags of every
                                 name: OUT/REL.scopes.ae, (python/scopes (block TYPE "NAME" {"SYM" FLAGS ...} [CHILD
                                 ...])); bin/pyi scope-check's reference (PY5a)
  pydump.py symtable FILE        the file's symbol tables as (python/symtable SCOPE): (scope TYPE NAME [SYMBOL...]
                                 [CHILD...]), (symbol NAME [FLAG...]); PY5's reference, written here at PY2
  pydump.py code FILE            the CODE digest of one file

Every file is read as data: nothing of it is imported or run (compile() only compiles).
"""
import ast
import hashlib
import io
import os
import re
import resource
import sys
import time
import tokenize
import types
import warnings
import dis
import marshal
import bisect
import symtable as symtable_mod

# ---- the spec's vocabulary --------------------------------------------------------------------------------------

BINOP = {ast.Add: "+", ast.Sub: "-", ast.Mult: "*", ast.MatMult: "mat-mult", ast.Div: "/", ast.Mod: "%",
         ast.Pow: "**", ast.LShift: "<<", ast.RShift: ">>", ast.BitOr: "bit-or", ast.BitXor: "bit-xor",
         ast.BitAnd: "bit-and", ast.FloorDiv: "floor-div"}
UNARY = {ast.Invert: "bit-not", ast.Not: "not", ast.UAdd: "+", ast.USub: "-"}
BOOL = {ast.And: "and", ast.Or: "or"}
CMP = {ast.Eq: "==", ast.NotEq: "!=", ast.Lt: "<", ast.LtE: "<=", ast.Gt: ">", ast.GtE: ">=", ast.Is: "is",
       ast.IsNot: "is-not", ast.In: "in", ast.NotIn: "not-in"}
CONV = {115: "s", 114: "r", 97: "a"}
NOT_SYMBOLS = {"nil", "true", "false"}
PLAIN_INT = re.compile(r"(?:0|[1-9][0-9]*)\Z")


def qstr(s):
    """A Clojure string literal"""
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def ident(name):
    if name in NOT_SYMBOLS:
        return qstr(name)
    return name


def vec(xs):
    return "[" + " ".join(xs) + "]"


def form(head, *fields, opts=(), meta=None):
    parts = [head]
    fs = list(fields)
    while fs and fs[-1] is None:
        fs.pop()
    parts += ["nil" if f is None else f for f in fs]
    for k, v in opts:
        if v is not None:
            parts += [k, v]
    if meta:
        parts.append("{" + " ".join("%s %s" % kv for kv in meta) + "}")
    return "(" + " ".join(parts) + ")"


def nocomment(s):
    """s without its comments (from # to the end of its line); s holds no string literal (it is the text around a
    replacement field's expression)"""
    return re.sub(r"#[^\r\n]*", "", s)


class Source:
    """The decoded text and the conversion of the ast's (line, UTF-8 byte column) to an index into it"""

    def __init__(self, text):
        self.text = text
        self.starts = [0]
        for m in re.finditer(r"\r\n|\r|\n", text):
            self.starts.append(m.end())
        self.lines = [text[self.starts[i]:(self.starts[i + 1] if i + 1 < len(self.starts) else len(text))]
                      for i in range(len(self.starts))]

    def index(self, line, col):
        ln = self.lines[line - 1]
        if ln.isascii():
            c = col
        else:
            c = len(ln.encode("utf-8")[:col].decode("utf-8", "replace"))
        return self.starts[line - 1] + c

    def cindex(self, line, col):
        """tokenize's (line, character column)"""
        return self.starts[line - 1] + col if line <= len(self.starts) else len(self.text)

    def span(self, n):
        return self.index(n.lineno, n.col_offset), self.index(n.end_lineno, n.end_col_offset)

    def seg(self, n):
        a, b = self.span(n)
        return self.text[a:b]


class Writer:
    def __init__(self, src, tc_tree, full=False):
        self.src = src
        self.full = full
        self.fields = {}  # start index -> FormattedValue or Interpolation
        self.nodes = 0
        self.tc = {}  # (type, lineno, col) -> type_comment, from the type_comments parse
        if tc_tree is not None:
            for n in ast.walk(tc_tree):
                c = getattr(n, "type_comment", None)
                if c is not None:
                    self.tc[(type(n).__name__, n.lineno, n.col_offset)] = c

    def meta_tc(self, n, extra=()):
        m = list(extra)
        c = self.tc.get((type(n).__name__, n.lineno, n.col_offset))
        if c is not None:
            m.append((":type-comment", qstr(c)))
        return m

    # ---- statements
    def body(self, stmts):
        return vec([self.stmt(s) for s in stmts])

    def orelse(self, n):
        if not n.orelse:
            return None
        if (isinstance(n, ast.If) and len(n.orelse) == 1 and isinstance(n.orelse[0], ast.If)
                and self.src.text.startswith("elif", self.src.index(n.orelse[0].lineno, n.orelse[0].col_offset))):
            return vec([self.stmt(n.orelse[0], [(":elif", "true")])])
        return self.body(n.orelse)

    def _stmt(self, n, extra=()):
        self.nodes += 1
        e, b, t = self.expr, self.body, self.target
        k = type(n)
        if k in (ast.FunctionDef, ast.AsyncFunctionDef):
            return form("function-def" if k is ast.FunctionDef else "async-function-def", ident(n.name),
                        self.arguments(n.args), b(n.body),
                        opts=[(":decorators", vec([e(d) for d in n.decorator_list]) if n.decorator_list else None),
                              (":returns", e(n.returns) if n.returns else None),
                              (":type-params", self.tparams(n.type_params))], meta=self.meta_tc(n, extra))
        if k is ast.ClassDef:
            return form("class-def", ident(n.name), b(n.body),
                        opts=[(":bases", vec([e(x) for x in n.bases]) if n.bases else None),
                              (":keywords", vec([self.keyword(x) for x in n.keywords]) if n.keywords else None),
                              (":decorators", vec([e(d) for d in n.decorator_list]) if n.decorator_list else None),
                              (":type-params", self.tparams(n.type_params))], meta=list(extra))
        if k is ast.Return:
            return form("return", e(n.value) if n.value else None, meta=list(extra))
        if k is ast.Delete:
            return form("delete", vec([self.target(x, "del") for x in n.targets]), meta=list(extra))
        if k is ast.Assign:
            return form("assign", vec([t(x) for x in n.targets]), e(n.value), meta=self.meta_tc(n, extra))
        if k is ast.TypeAlias:
            return form("type-alias", t(n.name), e(n.value), opts=[(":type-params", self.tparams(n.type_params))],
                        meta=list(extra))
        if k is ast.AugAssign:
            return form("aug-assign", t(n.target), BINOP[type(n.op)], e(n.value), meta=list(extra))
        if k is ast.AnnAssign:
            simple = "false" if (isinstance(n.target, ast.Name) and not n.simple) else None
            return form("ann-assign", t(n.target), e(n.annotation), e(n.value) if n.value else None,
                        opts=[(":simple", simple)], meta=list(extra))
        if k in (ast.For, ast.AsyncFor):
            return form("for" if k is ast.For else "async-for", t(n.target), e(n.iter), b(n.body),
                        opts=[(":else", self.orelse(n))], meta=self.meta_tc(n, extra))
        if k is ast.While:
            return form("while", e(n.test), b(n.body), opts=[(":else", self.orelse(n))], meta=list(extra))
        if k is ast.If:
            return form("if", e(n.test), b(n.body), opts=[(":else", self.orelse(n))], meta=list(extra))
        if k in (ast.With, ast.AsyncWith):
            items = vec([form("withitem", e(i.context_expr), t(i.optional_vars) if i.optional_vars else None)
                         for i in n.items])
            return form("with" if k is ast.With else "async-with", items, b(n.body), meta=self.meta_tc(n, extra))
        if k is ast.Match:
            return form("match", e(n.subject), vec([self.case(c) for c in n.cases]), meta=list(extra))
        if k is ast.Raise:
            return form("raise", e(n.exc) if n.exc else None, opts=[(":from", e(n.cause) if n.cause else None)],
                        meta=list(extra))
        if k in (ast.Try, ast.TryStar):
            hs = []
            for h in n.handlers:
                self.nodes += 1
                hs.append(self.posit(form("except-handler", b(h.body), opts=[(":type", e(h.type) if h.type else None),
                                                                             (":name", ident(h.name) if h.name else None)]), h))
            return form("try" if k is ast.Try else "try-star", b(n.body), vec(hs),
                        opts=[(":else", b(n.orelse) if n.orelse else None),
                              (":finally", b(n.finalbody) if n.finalbody else None)], meta=list(extra))
        if k is ast.Assert:
            return form("assert", e(n.test), e(n.msg) if n.msg else None, meta=list(extra))
        if k is ast.Import:
            return form("import", vec([self.alias(a) for a in n.names]), meta=list(extra))
        if k is ast.ImportFrom:
            return form("import-from", n.module if n.module else "nil", vec([self.alias(a) for a in n.names]),
                        opts=[(":level", str(n.level) if n.level else None)], meta=list(extra))
        if k is ast.Global:
            return form("global", vec([ident(x) for x in n.names]), meta=list(extra))
        if k is ast.Nonlocal:
            return form("nonlocal", vec([ident(x) for x in n.names]), meta=list(extra))
        if k is ast.Expr:
            return form("expr", e(n.value), meta=list(extra))
        if k is ast.Pass:
            return form("pass", meta=list(extra))
        if k is ast.Break:
            return form("break", meta=list(extra))
        if k is ast.Continue:
            return form("continue", meta=list(extra))
        raise ValueError("unknown statement " + k.__name__)

    def _alias(self, a):
        self.nodes += 1
        if a.asname is None:
            return ident(a.name) if a.name not in NOT_SYMBOLS else form("alias", qstr(a.name))
        return form("alias", ident(a.name), opts=[(":as", ident(a.asname))])

    def _keyword(self, k):
        self.nodes += 1
        return form("keyword", ident(k.arg) if k.arg else "nil", self.expr(k.value))

    def _arg(self, a):
        self.nodes += 1
        c = self.tc.get(("arg", a.lineno, a.col_offset))
        if a.annotation is None and c is None:
            return ident(a.arg) if a.arg not in NOT_SYMBOLS else form("arg", qstr(a.arg))
        return form("arg", ident(a.arg), self.expr(a.annotation) if a.annotation else None,
                    meta=[(":type-comment", qstr(c))] if c is not None else None)

    def arguments(self, x):
        self.nodes += 1
        kwd = None
        if any(d is not None for d in x.kw_defaults):
            kwd = vec(["nil" if d is None else self.expr(d) for d in x.kw_defaults])
        return form("arguments", opts=[
            (":posonly", vec([self.arg(a) for a in x.posonlyargs]) if x.posonlyargs else None),
            (":args", vec([self.arg(a) for a in x.args]) if x.args else None),
            (":vararg", self.arg(x.vararg) if x.vararg else None),
            (":kwonly", vec([self.arg(a) for a in x.kwonlyargs]) if x.kwonlyargs else None),
            (":kw-defaults", kwd),
            (":kwarg", self.arg(x.kwarg) if x.kwarg else None),
            (":defaults", vec([self.expr(d) for d in x.defaults]) if x.defaults else None)])

    def tparams(self, ps):
        if not ps:
            return None
        out = []
        for p in ps:
            self.nodes += 1
            d = self.expr(p.default_value) if p.default_value else None
            if isinstance(p, ast.TypeVar):
                f = form("type-var", ident(p.name), opts=[(":bound", self.expr(p.bound) if p.bound else None),
                                                           (":default", d)])
            elif isinstance(p, ast.ParamSpec):
                f = form("param-spec", ident(p.name), opts=[(":default", d)])
            else:
                f = form("type-var-tuple", ident(p.name), opts=[(":default", d)])
            out.append(self.posit(f, p))
        return vec(out)

    def case(self, c):
        self.nodes += 1
        return form("match-case", self.pattern(c.pattern), self.body(c.body),
                    opts=[(":guard", self.expr(c.guard) if c.guard else None)])

    def _pattern(self, p):
        self.nodes += 1
        k = type(p)
        if k is ast.MatchValue:
            return form("match-value", self.expr(p.value))
        if k is ast.MatchSingleton:
            return form("match-singleton", {None: "None", True: "True", False: "False"}[p.value])
        if k is ast.MatchSequence:
            return form("match-sequence", vec([self.pattern(x) for x in p.patterns]))
        if k is ast.MatchMapping:
            return form("match-mapping", vec([self.expr(x) for x in p.keys]), vec([self.pattern(x) for x in p.patterns]),
                        opts=[(":rest", ident(p.rest) if p.rest else None)])
        if k is ast.MatchClass:
            return form("match-class", self.expr(p.cls), vec([self.pattern(x) for x in p.patterns]),
                        opts=[(":kwd-attrs", vec([ident(x) for x in p.kwd_attrs]) if p.kwd_attrs else None),
                              (":kwd-patterns", vec([self.pattern(x) for x in p.kwd_patterns]) if p.kwd_patterns else None)])
        if k is ast.MatchStar:
            return form("match-star", ident(p.name) if p.name else None)
        if k is ast.MatchAs:
            return form("match-as", self.pattern(p.pattern) if p.pattern else None, ident(p.name) if p.name else None)
        if k is ast.MatchOr:
            return form("match-or", vec([self.pattern(x) for x in p.patterns]))
        raise ValueError("unknown pattern " + k.__name__)

    # ---- expressions
    def target(self, n, ctx="store"):
        return self.expr(n)

    def ctxmark(self, n):
        c = getattr(n, "ctx", None)
        if isinstance(c, ast.Store):
            return "store"
        if isinstance(c, ast.Del):
            return "del"
        return None

    def _expr(self, n):
        self.nodes += 1
        e = self.expr
        k = type(n)
        cm = self.ctxmark(n)
        lmeta = [(":ctx", ":" + cm)] if cm else None
        if k is ast.Name:
            s = ident(n.id)
            if n.id in NOT_SYMBOLS:
                return form("name", s, meta=lmeta)
            return ("^:%s " % cm if cm else "") + s
        if k is ast.Constant:
            return self.constant(n)
        if k in (ast.JoinedStr, ast.TemplateStr):
            return self.strings(n)
        if k is ast.BoolOp:
            return form("bool-op", BOOL[type(n.op)], vec([e(v) for v in n.values]))
        if k is ast.NamedExpr:
            return form("named-expr", e(n.target), e(n.value))
        if k is ast.BinOp:
            return form("bin-op", BINOP[type(n.op)], e(n.left), e(n.right))
        if k is ast.UnaryOp:
            return form("unary-op", UNARY[type(n.op)], e(n.operand))
        if k is ast.Lambda:
            return form("lambda", self.arguments(n.args), e(n.body))
        if k is ast.IfExp:
            return form("if-exp", e(n.test), e(n.body), e(n.orelse))
        if k is ast.Dict:
            return form("dict", vec(["nil" if x is None else e(x) for x in n.keys]), vec([e(v) for v in n.values]))
        if k is ast.Set:
            return form("set", vec([e(x) for x in n.elts]))
        if k in (ast.ListComp, ast.SetComp, ast.GeneratorExp):
            h = {ast.ListComp: "list-comp", ast.SetComp: "set-comp", ast.GeneratorExp: "generator-exp"}[k]
            return form(h, e(n.elt), self.gens(n.generators))
        if k is ast.DictComp:
            return form("dict-comp", e(n.key), e(n.value), self.gens(n.generators))
        if k is ast.Await:
            return form("await", e(n.value))
        if k is ast.Yield:
            return form("yield", e(n.value) if n.value else None)
        if k is ast.YieldFrom:
            return form("yield-from", e(n.value))
        if k is ast.Compare:
            return form("compare", e(n.left), vec([CMP[type(o)] for o in n.ops]), vec([e(c) for c in n.comparators]))
        if k is ast.Call:
            return form("call", e(n.func), vec([e(a) for a in n.args]),
                        opts=[(":keywords", vec([self.keyword(x) for x in n.keywords]) if n.keywords else None)])
        if k is ast.Attribute:
            return form("attribute", e(n.value), ident(n.attr), meta=lmeta)
        if k is ast.Subscript:
            return form("subscript", e(n.value), e(n.slice), meta=lmeta)
        if k is ast.Starred:
            return form("starred", e(n.value), meta=lmeta)
        if k is ast.List:
            return form("list", vec([e(x) for x in n.elts]), meta=lmeta)
        if k is ast.Tuple:
            return form("tuple", vec([e(x) for x in n.elts]), meta=lmeta)
        if k is ast.Slice:
            return form("slice", e(n.lower) if n.lower else None, e(n.upper) if n.upper else None,
                        e(n.step) if n.step else None)
        raise ValueError("unknown expression " + k.__name__)

    def gens(self, gs):
        out = []
        for g in gs:
            self.nodes += 1
            out.append(form("comprehension", self.expr(g.target), self.expr(g.iter),
                            opts=[(":ifs", vec([self.expr(i) for i in g.ifs]) if g.ifs else None),
                                  (":async", "true" if g.is_async else None)]))
        return vec(out)

    # ---- literals (§3)
    def constant(self, n):
        v = n.value
        if v is None or v is True or v is False or v is Ellipsis:
            return {None: "None", True: "True", False: "False"}.get(v, "...") if v is not Ellipsis else "..."
        if isinstance(v, (str, bytes)):
            return self.strings(n)
        text = self.src.seg(n)
        if isinstance(v, bool):
            raise ValueError("bool constant")
        if isinstance(v, int):
            if ast.literal_eval(text) != v:
                raise ValueError("the spelling %r is not the value %r" % (text, v))
            return text if PLAIN_INT.match(text) else form("int", qstr(text))
        if isinstance(v, float):
            lv = ast.literal_eval(text)
            if not (lv == v or (lv != lv and v != v)):
                raise ValueError("the spelling %r is not the value %r" % (text, v))
            return form("float", qstr(text))
        if isinstance(v, complex):
            if ast.literal_eval(text) != v:
                raise ValueError("the spelling %r is not the value %r" % (text, v))
            return form("imag", qstr(text))
        raise ValueError("constant of type " + type(v).__name__)

    def strings(self, n):
        """A Constant str or bytes, a JoinedStr or a TemplateStr: its tokens scanned in the source (§3.2-§3.5)"""
        t = self.src.text
        a, z = self.src.span(n)
        parts = []
        i = a
        while i < z:
            c = t[i]
            if c in " \t\f\r\n":
                i += 1
                continue
            if c == "\\":
                i += 1
                continue
            if c == "#":
                while i < z and t[i] not in "\r\n":
                    i += 1
                continue
            j = i
            while t[j] in "rRbBuUfFtT":
                j += 1
            prefix = t[i:j]
            quote = t[j:j + 3] if t[j:j + 3] in ('"""', "'''") else t[j]
            if quote not in ('"', "'", '"""', "'''"):
                raise ValueError("no string token at %d: %r" % (i, t[i:i + 20]))
            k = j + len(quote)
            lower = prefix.lower()
            if "f" in lower or "t" in lower:
                items, k = self.fbody(k, quote, z, raw="r" in lower)
                head = "joined-str" if "f" in lower else "template-str"
                default = "f" if "f" in lower else "t"
                parts.append(form(head, vec(items), opts=[(":prefix", qstr(prefix) if prefix != default else None),
                                                           (":quote", qstr(quote) if quote != '"' else None)]))
            else:
                b0 = k
                while not t.startswith(quote, k):
                    k += 2 if t[k] == "\\" else 1
                body = t[b0:k]
                k += len(quote)
                if "b" in lower:
                    parts.append(form("bytes", qstr(body), opts=[(":prefix", qstr(prefix) if prefix != "b" else None),
                                                                (":quote", qstr(quote) if quote != '"' else None)]))
                elif prefix == "" and quote == '"' and "\\" not in body:
                    parts.append(qstr(body))
                else:
                    parts.append(form("str", qstr(body), opts=[(":prefix", qstr(prefix) if prefix else None),
                                                              (":quote", qstr(quote) if quote != '"' else None)]))
            i = k
        if not parts:
            raise ValueError("no string token in the node's span")
        if isinstance(n, ast.Constant):
            # the value from the spelling: the tokens evaluated together equal the ast's value
            toks = t[a:z]
            if ast.literal_eval("(" + toks + "\n)") != n.value:
                raise ValueError("the spelling of a string is not its value")
        return parts[0] if len(parts) == 1 else "(str-concat " + " ".join(parts) + ")"

    def fbody(self, k, quote, z, spec_end=None, raw=False):
        """The parts of an f-string's or t-string's body from k to its closing quote (or to spec_end, a format
        spec's end): literal text as written, and replacement fields"""
        t = self.src.text
        items = []
        lit = k
        while True:
            if spec_end is not None:
                if k >= spec_end:
                    break
            elif t.startswith(quote, k):
                break
            c = t[k]
            if c == "\\":
                nxt = t[k + 1:k + 2]
                if not raw and t[k + 1:k + 3] == "N{":
                    k = t.index("}", k) + 1
                elif nxt in ("{", "}") or (raw and nxt not in ("\\", "'", '"')):
                    k += 1
                else:
                    k += 2
                continue
            if c == "{" and t[k + 1:k + 2] == "{" and spec_end is None:
                k += 2
                continue
            if c == "}" and t[k + 1:k + 2] == "}" and spec_end is None:
                k += 2
                continue
            if c == "{":
                f = self.fields.get(k)
                if f is None:
                    raise ValueError("no replacement field at %d" % k)
                if lit < k:
                    items.append(qstr(t[lit:k]))
                items.append(self.field(f, raw))
                k = self.src.span(f)[1]
                lit = k
                continue
            k += 1
        if lit < k:
            items.append(qstr(t[lit:k]))
        if spec_end is None:
            k += len(quote)
        return items, k

    def skip(self, k, more=""):
        """k past spaces, line ends, comments, backslash continuations (and the characters of more)"""
        t = self.src.text
        while True:
            c = t[k]
            if c in " \t\r\n\f" or c in more:
                k += 1
            elif c == "#":
                while t[k] not in "\r\n":
                    k += 1
            elif c == "\\" and t[k + 1:k + 2] in ("\r", "\n"):
                k += 2
            else:
                return k

    def _field(self, f, raw):
        t = self.src.text
        fs, fe = self.src.span(f)
        self.nodes += 1
        vs, ve = self.src.span(f.value)
        k = self.skip(ve, ")")
        debug = None
        if t[k] == "=" and t[k + 1:k + 2] != "=":
            k = self.skip(k + 1)
            # the debug text is the source's, less its comments (CPython drops them: f"{1+2 = # c\n}" gives "1+2 = \n")
            debug = nocomment(t[fs + 1:vs]) + t[vs:ve] + nocomment(t[ve:k])
        conv = None
        if t[k] == "!":
            conv = t[k + 1]
            k += 2
            while t[k] in " \t\r\n\f":
                k += 1
        spec = None
        if t[k] == ":":
            if f.format_spec is None:
                raise ValueError("a ':' with no format spec")
            items, _ = self.fbody(k + 1, None, fe - 1, spec_end=fe - 1, raw=raw)
            spec = vec(items)
        if conv is not None and conv not in "sra":
            raise ValueError("conversion " + conv)
        want = {None: -1, "s": 115, "r": 114, "a": 97}[conv]
        implied = want == -1 and debug is not None and f.format_spec is None
        if f.conversion != want and not (implied and f.conversion == 114):
            raise ValueError("the conversion written is not the ast's")
        opts = [(":conversion", conv), (":format-spec", spec), (":debug", qstr(debug) if debug is not None else None)]
        if isinstance(f, ast.Interpolation):
            return form("interpolation", self.expr(f.value), qstr(f.str), opts=opts)
        return form("formatted-value", self.expr(f.value), opts=opts)

    # ---- mode :full (§11): every node with attributes carries :pos, written in its long spelling (§1.4)
    def posit(self, text, n, short=None):
        """text with n's position in its trailing map (mode :full); a short spelling first made long"""
        if not self.full or getattr(n, "end_lineno", None) is None:
            return text
        pos = ":pos [%d %d %d %d]" % (n.lineno, n.col_offset, n.end_lineno, n.end_col_offset)
        if not text.startswith("("):
            ctx = None
            if text.startswith("^:"):
                ctx, text = text[2:].split(" ", 1)
            head = short
            if head == "const" and PLAIN_INT.match(text):
                head, text = "int", qstr(text)
            elif head == "const" and text.startswith('"'):
                head = "str"
            text = "(%s %s)" % (head, text)
            if ctx:
                pos = ":ctx :%s %s" % (ctx, pos)
        if text.endswith("})"):
            return text[:-2] + " " + pos + "})"
        return text[:-1] + " {" + pos + "})"

    def stmt(self, n, extra=()):
        return self.posit(self._stmt(n, extra), n)

    def expr(self, n):
        return self.posit(self._expr(n), n, "name" if isinstance(n, ast.Name) else "const")

    def pattern(self, p):
        return self.posit(self._pattern(p), p)

    def alias(self, a):
        return self.posit(self._alias(a), a, "alias")

    def keyword(self, k):
        return self.posit(self._keyword(k), k)

    def arg(self, a):
        return self.posit(self._arg(a), a, "arg")

    def field(self, f, raw):
        return self.posit(self._field(f, raw), f)

    def module(self, tree, rel, extra):
        for n in ast.walk(tree):
            if isinstance(n, (ast.FormattedValue, ast.Interpolation)):
                self.fields[self.src.span(n)[0]] = n
        meta = [(":file", qstr(rel)), (":mode", ":full" if self.full else ":lines")] + list(extra)
        return form("python/module", self.body(tree.body), meta=meta)


# ---- the code digest (level 3) ----------------------------------------------------------------------------------

SUPER = {"LOAD_FAST_LOAD_FAST": ("LOAD_FAST", "LOAD_FAST"),
         "LOAD_FAST_BORROW_LOAD_FAST_BORROW": ("LOAD_FAST", "LOAD_FAST"),
         "STORE_FAST_LOAD_FAST": ("STORE_FAST", "LOAD_FAST"),
         "STORE_FAST_STORE_FAST": ("STORE_FAST", "STORE_FAST")}


def canon(v):
    """A constant's text independent of the hash seed: a frozenset's elements sorted (with -I, PYTHONHASHSEED is not
    read, so two runs order a set constant differently)"""
    if isinstance(v, frozenset):
        return "frozenset({" + ", ".join(sorted(canon(x) for x in v)) + "})"
    if isinstance(v, tuple):
        return "(" + ", ".join(canon(x) for x in v) + ("," if len(v) == 1 else "") + ")"
    return repr(v)


def norm_code(co):
    """A code object without positions: its instructions with NOPs dropped, the superinstructions the compiler fuses
    by line split, LOAD_FAST_BORROW as LOAD_FAST, a class body's __firstlineno__ as a placeholder, each instruction's
    exception handler, the names and flags, nested code objects likewise. CPython fuses and inserts by line, so
    these are the parts of 3.14's code that depend on the lines a statement spans (probed on Lib/, 2026-11-03)."""
    out, offmap = [], {}
    ins = list(dis.get_instructions(co))
    for i, x in enumerate(ins):
        offmap[x.offset] = len(out)
        op = x.opname
        if op == "NOP":
            continue
        if op in SUPER:
            a, b = SUPER[op]
            u, v = x.argrepr.split(", ")
            out += [(a, u), (b, v)]
            continue
        if op == "LOAD_FAST_BORROW":
            op = "LOAD_FAST"
        arg = x.argrepr
        if op == "LOAD_CONST" and not isinstance(x.argval, types.CodeType):
            arg = canon(x.argval)
        if isinstance(x.argval, types.CodeType):
            arg = norm_code(x.argval)
        if (op in ("LOAD_CONST", "LOAD_SMALL_INT") and i + 1 < len(ins) and ins[i + 1].opname == "STORE_NAME"
                and ins[i + 1].argval == "__firstlineno__"):
            op, arg = "LOAD_CONST", "<firstlineno>"
        if op == "LOAD_SMALL_INT":
            op, arg = "LOAD_CONST", repr(x.argval)
        out.append((op, arg))
    offs = sorted(offmap)

    def at(o):
        j = bisect.bisect_left(offs, o)
        return offmap[offs[j]] if j < len(offs) else len(out)
    hand = [None] * len(out)
    for e in dis.Bytecode(co).exception_entries:
        for j in range(at(e.start), at(e.end)):
            hand[j] = (at(e.target), e.depth, e.lasti)
    return (co.co_name, co.co_qualname, co.co_flags, co.co_argcount, co.co_posonlyargcount, co.co_kwonlyargcount,
            co.co_names, co.co_varnames, co.co_freevars, co.co_cellvars, tuple(out), tuple(hand))


def code_digest(data, name):
    try:
        co = compile(data, name, "exec", dont_inherit=True)
    except (SyntaxError, ValueError) as e:
        return "compile-error: " + str(getattr(e, "msg", e)).replace("\t", " ").replace("\n", " ")
    return hashlib.sha256(repr(norm_code(co)).encode("utf-8", "surrogateescape")).hexdigest()


# ---- the modes ----------------------------------------------------------------------------------------------------

def error_form(e):
    opts = []
    if getattr(e, "lineno", None) is not None:
        opts.append((":pos", vec([str(x) if x is not None else "nil"
                                  for x in (e.lineno, e.offset, e.end_lineno, e.end_offset)])))
    if getattr(e, "text", None) is not None:
        opts.append((":text", qstr(e.text)))
    msg = getattr(e, "msg", None) or str(e)
    return form("python/error", type(e).__name__, qstr(msg), opts=opts)


def decode(data):
    enc, _ = tokenize.detect_encoding(io.BytesIO(data).readline)
    text = data.decode(enc)
    if text.startswith("﻿"):
        text = text[1:]
    return text


TOKEN_KIND = {t: ":" + n.lower().replace("_", "-") for t, n in tokenize.tok_name.items()}
MIDDLE = (tokenize.FSTRING_MIDDLE, tokenize.TSTRING_MIDDLE)
GAP = re.compile(r"(?:[ \t\f]|\\(?:\r\n|\r|\n))*\Z")


def tokens(data, src):
    """The file's tokens (tokenize.tokenize, E5) as a flat vector KIND TEXT KIND TEXT ..., each TEXT the source's
    slice (tokenize's columns count characters, the ast's UTF-8 bytes), the text between two tokens a :ws entry (spaces, tabs, form feeds, backslash continuations), so that the
    texts concatenated are the decoded file exactly (§11). An f-string's or t-string's middle runs to the next token
    (tokenize's string and end undouble {{ and }})."""
    toks = [t for t in tokenize.tokenize(io.BytesIO(data).readline) if t.type != tokenize.ENCODING]
    t = src.text
    out = []
    prev = 0
    starts = [src.cindex(*tk.start) for tk in toks]
    for i, tk in enumerate(toks):
        a = max(starts[i], prev)
        if tk.type in MIDDLE and i + 1 < len(toks):
            z = starts[i + 1]
        else:
            z = src.cindex(*tk.end)
            z = max(z, a)
        if a > prev:
            gap = t[prev:a]
            if not GAP.match(gap):
                raise ValueError("text between tokens that is not white space: %r" % gap[:40])
            out += [":ws", qstr(gap)]
        out += [TOKEN_KIND[tk.type], qstr(t[a:z])]
        prev = z
    if prev < len(t):
        gap = t[prev:]
        if not GAP.match(gap):
            raise ValueError("text after the last token that is not white space: %r" % gap[:40])
        out += [":ws", qstr(gap)]
    return vec(out)


def tree_one(path, rel, full=False):
    """{:text :status :ast :code :marshal :sha :nodes}"""
    data = open(path, "rb").read()
    r = {"sha": hashlib.sha256(data).hexdigest(), "ast": "-", "code": "-", "marshal": "-", "nodes": 0}
    try:
        tree = ast.parse(data, rel)
    except (SyntaxError, ValueError) as e:
        return dict(r, text=error_form(e) + "\n", status="error")
    extra = []
    try:
        tc = ast.parse(data, rel, type_comments=True)
    except SyntaxError:
        tc = None
        extra.append((":type-comments", ":rejected"))
    if tc is not None and tc.type_ignores:
        extra.append((":type-ignores", vec([form("type-ignore", str(t.lineno), qstr(t.tag)) for t in tc.type_ignores])))
    enc, _ = tokenize.detect_encoding(io.BytesIO(data).readline)
    src = Source(decode(data))
    if full:
        if enc not in ("utf-8", "utf-8-sig"):
            extra.append((":encoding", qstr(enc)))
        if data.startswith(b"\xef\xbb\xbf"):
            extra.append((":bom", "true"))
        extra.append((":tokens", tokens(data, src)))
    w = Writer(src, tc, full)
    text = w.module(tree, rel, extra)
    try:
        co = compile(data, rel, "exec", dont_inherit=True)
        r["marshal"] = hashlib.sha256(marshal.dumps(co)).hexdigest()
    except (SyntaxError, ValueError) as e:
        r["marshal"] = "compile-error"
    r.update(text=text + "\n", status="ok", nodes=w.nodes,
             ast=hashlib.sha256(ast.dump(tree).encode("utf-8", "surrogateescape")).hexdigest(),
             code=code_digest(data, rel))
    return r


def mode_tree(args):
    base = args[args.index("--base") + 1]
    lst = args[args.index("--list") + 1]
    out = args[args.index("--out") + 1]
    full = "--full" in args
    tsv = open(args[args.index("--tsv") + 1], "w", encoding="utf-8") if "--tsv" in args else None
    warnings.simplefilter("ignore")
    for rel in open(lst, encoding="utf-8").read().split("\n"):
        if not rel:
            continue
        t0 = time.perf_counter()
        try:
            r = tree_one(os.path.join(base, rel), rel, full)
        except RecursionError:
            r = dict(text=form("python/error", "HelperError", qstr("recursion")) + "\n", status="helper-error")
        except Exception as e:  # the helper's own failure: reported, the run goes on
            msg = "%s: %s" % (type(e).__name__, e)
            r = dict(text=form("python/error", "HelperError", qstr(msg)) + "\n", status="helper-error")
        dst = os.path.join(out, rel + ".ae")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(dst, "w", encoding="utf-8", newline="\n") as f:
            f.write(r["text"])
        secs = time.perf_counter() - t0
        rss = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
        if tsv:
            tsv.write("%s\t%s\t%s\t%s\t%s\t%s\t%d\t%.4f\t%d\n" % (rel, r["status"], r.get("ast", "-"), r.get("code", "-"),
                                                             r.get("marshal", "-"), r.get("sha", "-"), r.get("nodes", 0),
                                                             secs, rss))
    if tsv:
        tsv.close()


def mode_symtable(path):
    data = open(path, "rb").read()
    text = decode(data)

    def scope(t):
        syms = []
        for s in sorted(t.get_symbols(), key=lambda s: s.get_name()):
            flags = [f for f in ("referenced", "imported", "parameter", "global", "nonlocal", "declared_global",
                                 "local", "annotated", "free", "assigned", "namespace", "type_parameter")
                     if getattr(s, "is_" + f)()]
            syms.append(form("symbol", ident(s.get_name()), vec([":" + f.replace("_", "-") for f in flags])))
        kids = [scope(c) for c in t.get_children()]
        return form("scope", ":" + str(t.get_type()).split(".")[-1].lower().replace("_", "-"), qstr(t.get_name()),
                    vec(syms), vec(kids))
    try:
        top = symtable_mod.symtable(text, path, "exec")
    except SyntaxError as e:
        print(error_form(e))
        return
    print(form("python/symtable", scope(top)))


BLOCK_TYPES = {0: ":function", 1: ":class", 2: ":module", 3: ":annotation", 4: ":type-alias", 5: ":type-parameters",
               6: ":type-variable"}


def scope_form(e):
    """(block TYPE "NAME" {"SYMBOL" FLAGS ...} [CHILD ...]): CPython's own symbol table, the raw flags of each name
    (DEF_* | scope << SCOPE_OFF, Include/internal/pycore_symtable.h), the children in its order"""
    syms = " ".join("%s %d" % (qstr(k), v) for k, v in sorted(e.symbols.items()))
    return "(block %s %s {%s} [%s])" % (BLOCK_TYPES[e.type], qstr(e.name), syms, " ".join(scope_form(c) for c in e.children))


def mode_scopes(args):
    """scopes --base DIR --list FILE --out DIR: each file's symbol table, OUT/REL.scopes.ae: (python/scopes BLOCK), or
    (python/error ...) when ast.parse or the symbol table rejects it"""
    import _symtable
    base, lst, out = args[args.index("--base") + 1], args[args.index("--list") + 1], args[args.index("--out") + 1]
    warnings.simplefilter("ignore")
    for rel in [l for l in open(lst, encoding="utf-8").read().split("\n") if l]:
        data = open(os.path.join(base, rel), "rb").read()
        try:
            text = "(python/scopes %s)" % scope_form(_symtable.symtable(decode(data), rel, "exec"))
        except (SyntaxError, ValueError) as e:
            text = error_form(e)
        except Exception as e:
            text = form("python/error", "HelperError", qstr("%s: %s" % (type(e).__name__, e)))
        dst = os.path.join(out, rel + ".scopes.ae")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(dst, "w", encoding="utf-8", newline="\n") as f:
            f.write(text + "\n")


def main():
    sys.setrecursionlimit(20000)
    a = sys.argv[1:]
    if a[0] == "tree":
        mode_tree(a[1:])
    elif a[0] == "scopes":
        mode_scopes(a[1:])
    elif a[0] == "symtable":
        mode_symtable(a[1])
    elif a[0] == "code":
        print(code_digest(open(a[1], "rb").read(), a[1]))
    else:
        raise SystemExit(__doc__)


if __name__ == "__main__":
    main()

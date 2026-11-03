#!/usr/bin/env python3
"""The checklist of doc/python/SPEC.md (§13) against Parser/Python.asdl and the oracle's ast module.

usage: python3 -I -S -B spec_check.py SPEC.md PYTHON.ASDL OUTDIR

Run by bin/repr-python spec-check, isolated (-I: no user site, no script directory or current directory on the
path), on the pinned interpreter. It reads ASDL with its own small parser (the tarball's Parser/asdl.py is not
imported: the tarball is data, read in place), then requires:

  1. every type, constructor and field of Python.asdl has a row of the spec's checklist and every row names one,
     every constructor's fields listed in full, every type with attributes an *attributes* row naming them all;
  2. every slot is one of N (positional), :key (an option), meta:key, spelling or dropped; a constructor's
     positional slots are 1..N, each once; no option key twice in one constructor;
  3. every head is used once within its ASDL type's category (the node types share one namespace, the
     operator and context types each their own);
  4. the oracle's ast module agrees with Python.asdl: each constructor's class exists with the same _fields
     and _attributes, and every concrete node class of ast (but the deprecated aliases) is in Python.asdl;
  5. every worked example of the spec (';; py: SOURCE' in a forms block, ';; |' continuing it) is accepted by
     ast.parse and compiled (a valid program, not only a parse), and every ast class in its tree, but those
     with short spellings or written as symbols (Name, Constant, alias, arg, the contexts and the operators),
     the top Module and the format specs (vectors in the forms), has its head in the example's forms.

It writes every forms block's text to OUTDIR/NNN.forms (the script's caller reads them with Arbace's reader) and
prints one line per failure and a summary; exit 1 when anything fails.
"""
import ast
import os
import re
import sys

# --- ASDL -------------------------------------------------------------------------------------------------------

TOKEN = re.compile(r"\s*(?:(--[^\n]*)|([A-Za-z_][A-Za-z0-9_]*)|(\?\*|[=|(),*?{}]))")


def asdl_tokens(text):
    pos, out = 0, []
    while True:
        while pos < len(text) and text[pos].isspace():
            pos += 1
        if pos >= len(text):
            return out
        m = TOKEN.match(text, pos)
        if not m or m.end() == pos:
            raise SyntaxError("ASDL: unexpected %r at %d" % (text[pos:pos + 20], pos))
        pos = m.end()
        if m.group(1):
            continue
        out.append(m.group(2) or m.group(3))


def parse_asdl(text):
    """{type: {'ctors': {name: [(ftype, qual, fname)]}, 'product': bool, 'attributes': [...]}}"""
    toks = asdl_tokens(text)
    i = 0

    def take(want=None):
        nonlocal i
        t = toks[i]
        if want is not None and t != want:
            raise SyntaxError("ASDL: expected %r, got %r (token %d)" % (want, t, i))
        i += 1
        return t

    def fields():
        take("(")
        out = []
        while True:
            ftype = take()
            qual = ""
            if toks[i] in ("*", "?", "?*"):
                qual = take()
            out.append((ftype, qual, take()))
            if toks[i] == ",":
                take(",")
                continue
            take(")")
            return out

    take("module")
    take()
    take("{")
    types = {}
    while toks[i] != "}":
        name = take()
        take("=")
        t = {"ctors": {}, "product": False, "attributes": []}
        if toks[i] == "(":
            t["product"] = True
            t["ctors"][name] = fields()
        else:
            while True:
                c = take()
                t["ctors"][c] = fields() if toks[i] == "(" else []
                if toks[i] == "|":
                    take("|")
                    continue
                break
        if toks[i] == "attributes":
            take("attributes")
            t["attributes"] = fields()
        types[name] = t
    return types


# --- the spec's table -------------------------------------------------------------------------------------------

ROW = re.compile(r"^\| `([a-z_]+)` \| (\*attributes\*|`[A-Za-z_]+`) \|([^|]*)\|([^|]*)\|\s*$")
FIELD = re.compile(r"`([a-z_]+)→([^`]+)`")
SLOT = re.compile(r"^(?:[1-9][0-9]*|:[a-z][a-z-]*|meta:[a-z][a-z-]*|spelling|dropped)$")


def checklist(spec):
    m = re.search(r"^## 13\. The checklist\n(.*?)^## ", spec, re.S | re.M)
    if not m:
        raise SystemExit("spec-check: no section '## 13. The checklist' in the spec")
    rows = []
    for n, line in enumerate(m.group(1).split("\n")):
        if not line.startswith("| `"):
            continue
        r = ROW.match(line)
        if not r:
            rows.append(("BAD", line))
            continue
        ctor = r.group(2).strip("`")
        heads = re.findall(r"`([^`]+)`", r.group(3))
        flds = FIELD.findall(r.group(4))
        rest = FIELD.sub("", r.group(4)).replace(",", "").strip()
        rows.append((r.group(1), ctor, heads, flds, rest, line))
    return rows


# --- the examples -----------------------------------------------------------------------------------------------

# the ast classes that have short spellings or are written as symbols; and those of the top form
EXEMPT = {"Module", "Name", "Constant", "alias", "arg", "Load", "Store", "Del"}


def examples(spec):
    blocks = re.findall(r"^```forms\n(.*?)^```$", spec, re.S | re.M)
    entries = []
    for b in blocks:
        cur = None
        for line in b.split("\n"):
            if line.startswith(";; py: ") or line == ";; py:":
                if cur:
                    entries.append(cur)
                cur = [line[7:], []]
            elif line.startswith(";; |") and cur is not None and not cur[1]:
                cur[0] += "\n" + line[5:]
            elif cur is not None:
                cur[1].append(line)
        if cur:
            entries.append(cur)
    return blocks, entries


def main():
    spec_path, asdl_path, out = sys.argv[1:4]
    spec = open(spec_path, encoding="utf-8").read()
    types = parse_asdl(open(asdl_path, encoding="utf-8").read())
    fails = []

    def fail(msg):
        fails.append(msg)
        print("FAIL", msg)

    rows = checklist(spec)
    seen = {}  # (type, ctor) -> row
    for row in rows:
        if row[0] == "BAD":
            fail("a checklist row does not parse: " + row[1])
            continue
        typ, ctor, heads, flds, rest, line = row
        if rest:
            fail("%s %s: text in the fields column that is no field: %r" % (typ, ctor, rest))
        if (typ, ctor) in seen:
            fail("%s %s: two rows" % (typ, ctor))
        seen[(typ, ctor)] = row
        if typ not in types:
            fail("%s: no such type in Python.asdl" % typ)
            continue
        t = types[typ]
        if ctor == "*attributes*":
            want = [f[2] for f in t["attributes"]]
            if not want:
                fail("%s: an attributes row, but the type has no attributes" % typ)
        elif ctor not in t["ctors"]:
            fail("%s %s: no such constructor in Python.asdl" % (typ, ctor))
            continue
        else:
            want = [f[2] for f in t["ctors"][ctor]]
        got = [f for f, _ in flds]
        for f in want:
            if f not in got:
                fail("%s %s: field %s of Python.asdl missing from the checklist" % (typ, ctor, f))
        for f in got:
            if f not in want:
                fail("%s %s: field %s of the checklist not in Python.asdl" % (typ, ctor, f))
        if len(set(got)) != len(got):
            fail("%s %s: a field listed twice" % (typ, ctor))
        if got and [f for f in got if f in want] != [f for f in want if f in got]:
            fail("%s %s: fields not in Python.asdl's order" % (typ, ctor))
        for f, s in flds:
            if not SLOT.match(s):
                fail("%s %s: field %s's slot %r is none of N, :key, meta:key, spelling, dropped" % (typ, ctor, f, s))
        if ctor != "*attributes*":
            pos = sorted(int(s) for _, s in flds if s.isdigit())
            if pos != list(range(1, len(pos) + 1)):
                fail("%s %s: positional slots %s are not 1..%d" % (typ, ctor, pos, len(pos)))
            keys = [s for _, s in flds if s.startswith(":")]
            if len(set(keys)) != len(keys):
                fail("%s %s: an option key twice" % (typ, ctor))
            if typ != "expr_context" and not heads:
                fail("%s %s: no head" % (typ, ctor))
            if typ == "expr_context" and heads:
                fail("%s %s: a context has no head (dropped)" % (typ, ctor))
    for typ, t in types.items():
        for c in t["ctors"]:
            if (typ, c) not in seen:
                fail("%s %s: constructor of Python.asdl missing from the checklist" % (typ, c))
        if t["attributes"] and (typ, "*attributes*") not in seen:
            fail("%s: its attributes have no row" % typ)

    # heads: unique within a category
    SYMBOL_TYPES = {"boolop", "operator", "unaryop", "cmpop", "expr_context"}
    cats = {}
    for row in rows:
        if row[0] == "BAD" or row[1] == "*attributes*":
            continue
        cat = row[0] if row[0] in SYMBOL_TYPES else "node"
        for h in row[2]:
            if h in cats.setdefault(cat, {}):
                fail("head %s used by %s and %s (category %s)" % (h, cats[cat][h], row[1], cat))
            cats[cat][h] = row[1]
    node_heads = set(cats.get("node", {}))

    # the oracle's ast module against Python.asdl
    deprecated = {"AugLoad", "AugStore", "ExtSlice", "Index", "Param", "Suite", "slice"}
    asdl_classes = {}
    for typ, t in types.items():
        for c, fl in t["ctors"].items():
            asdl_classes[c] = (typ, fl, t["attributes"])
        if not t["product"]:
            asdl_classes.setdefault(typ, None)
    for c, v in asdl_classes.items():
        cls = getattr(ast, c, None)
        if cls is None:
            fail("ast has no class %s (Python.asdl has)" % c)
            continue
        if v is None:
            continue
        typ, fl, attrs = v
        if tuple(f[2] for f in fl) != tuple(cls._fields):
            fail("ast.%s._fields %s differs from Python.asdl's %s" % (c, cls._fields, [f[2] for f in fl]))
        if tuple(f[2] for f in attrs) != tuple(getattr(cls, "_attributes", ())):
            fail("ast.%s._attributes %s differs from Python.asdl's" % (c, cls._attributes))
    for name in dir(ast):
        obj = getattr(ast, name)
        if isinstance(obj, type) and issubclass(obj, ast.AST) and obj is not ast.AST and name not in deprecated:
            if name not in asdl_classes:
                fail("ast.%s is not in Python.asdl" % name)

    # the examples
    os.makedirs(out, exist_ok=True)
    blocks, entries = examples(spec)
    for n, b in enumerate(blocks):
        with open(os.path.join(out, "%03d.forms" % n), "w", encoding="utf-8") as f:
            f.write(b)
    head_of = {}
    for row in rows:
        if row[0] != "BAD" and row[1] != "*attributes*" and row[2]:
            head_of[row[1]] = row[2]
    checked = 0
    for src, forms in entries:
        text = "\n".join(forms)
        try:
            tree = ast.parse(src + "\n")
            compile(src + "\n", "<spec>", "exec", dont_inherit=True)
        except SyntaxError as e:
            fail("example %r: ast.parse or compile rejects it: %s" % (src, e))
            continue
        checked += 1
        sym_tokens = set(re.findall(r"\(([^\s()\[\]{}\"]+)", text))
        # a format spec is a JoinedStr in the ast and a vector of parts in the forms (§3.4)
        specs = {id(n.format_spec) for n in ast.walk(tree)
                 if isinstance(n, (ast.FormattedValue, ast.Interpolation)) and n.format_spec is not None}
        for node in ast.walk(tree):
            c = type(node).__name__
            if id(node) in specs:
                continue
            if c in EXEMPT or type(node).__mro__[1].__name__ in SYMBOL_TYPES:
                continue
            hs = head_of.get(c)
            if hs is None:
                fail("example %r: ast class %s has no head in the checklist" % (src, c))
            elif not any(h in sym_tokens for h in hs):
                fail("example %r: ast class %s, head %s, not in its forms" % (src, c, " or ".join(hs)))
        for h in sym_tokens:
            if h not in node_heads and h not in ("str-concat", "python/error", "name", "alias", "arg"):
                fail("example %r: head (%s not in the checklist" % (src, h))

    nctors = sum(len(t["ctors"]) for t in types.values())
    nfields = sum(len(fl) for t in types.values() for fl in t["ctors"].values())
    nattr = sum(1 for t in types.values() if t["attributes"])
    print("spec-check: Python.asdl has %d types, %d constructors, %d fields, %d types with attributes; "
          "the checklist %d rows; ast %s; %d forms blocks, %d examples parsed and their heads present"
          % (len(types), nctors, nfields, nattr, len([r for r in rows if r[0] != "BAD"]),
             sys.version.split()[0], len(blocks), checked))
    if fails:
        print("spec-check: %d failure(s)" % len(fails))
        sys.exit(1)
    print("spec-check: every node and field of Python.asdl is in the checklist, and the oracle's ast agrees")


if __name__ == "__main__":
    main()

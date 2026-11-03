# unicodedata_table.py PYTHON_SRC OUT.go [--check]: the Unicode database's tables (PY6c), go/arbace/py's
# builtins_unicodedata_table.go, from CPython 3.14.10's own generated headers in the pinned tarball:
# Objects/unicodetype_db.h (str's character types and case mappings, Objects/unicodectype.c's data),
# Modules/unicodedata_db.h (the records, decompositions, compositions, the 3.2.0 changes) and
# Modules/unicodename_db.h (the names' DAWG, aliases, named sequences), all made by CPython's
# Tools/unicode/makeunicodedata.py from UCD 16.0.0. The arrays are taken as the headers spell them, no table
# is recomputed, so the runtime's lookups are the C code's (unicodectype.c, unicodedata.c) over the same data.
#
# OUT.go holds: the headers' #defines and scalar constants as Go constants; the name tables (categories,
# bidirectional classes, widths, decomposition prefixes, derived-name prefixes) as Go string slices; the switch
# functions (_PyUnicode_ToNumeric, _PyUnicode_IsWhitespace, _PyUnicode_IsLinebreak, normalization_3_2_0) and the
# struct arrays with doubles (change_records_3_2_0) as text constants, one entry a line, their numbers as the C
# literals spell them; every other array in ucdBlob, base64 of zlib (level 9) of the concatenation of records
#   NAME-LENGTH(1) NAME WIDTH(1: 1, 2 or 4 bytes, 0x80 set when signed) COUNT(4, little-endian) ELEMENTS(little-endian)
# a struct array flattened field by field (ucdArrays below names each array's fields). Deterministic for a given
# tarball; --check regenerates and requires OUT.go equal to it but for ucdBlob's text, compared decompressed (zlib's
# bytes may differ between zlib versions; the data may not).
# Run: python3 -I -S -B tools/pyi/unicodedata_table.py .tmp/repr-python/Python-3.14.10 OUT.go [--check]
import base64
import re
import struct
import sys
import zlib

src, out = sys.argv[1], sys.argv[2]
check = "--check" in sys.argv[3:]

TYPE = open(src + "/Objects/unicodetype_db.h", encoding="utf-8").read()
DATA = open(src + "/Modules/unicodedata_db.h", encoding="utf-8").read()
NAME = open(src + "/Modules/unicodename_db.h", encoding="utf-8").read()


def array_body(text, name):
    m = re.search(r"\b" + re.escape(name) + r"\[\] = \{", text)
    if not m:
        raise SystemExit("unicodedata_table: no array " + name)
    i, depth = m.end(), 1
    j = i
    while depth:
        c = text[j]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
        j += 1
    return text[i:j - 1]


def ints(text, name, fields=1, pad=None):
    """the array's integers, a struct flattened; pad: (inner count) to pad a struct's inner array to"""
    body = array_body(text, name)
    if pad is None:
        vals = re.findall(r"-?0x[0-9A-Fa-f]+|-?\d+", body)
        return [int(v, 0) for v in vals]
    out = []
    for rec in re.findall(r"\{\s*(-?\w+)\s*,\s*\{([^}]*)\}\s*\}", body):
        seq = [int(v, 0) for v in re.findall(r"0x[0-9A-Fa-f]+|\d+", rec[1])]
        out += [int(rec[0], 0)] + seq + [0] * (pad - len(seq))
    return out


def strings(text, name):
    body = array_body(text, name)
    return re.findall(r'"((?:[^"\\]|\\.)*)"', body)


def define(text, name):
    return re.search(r"#define " + name + r" (\S+)", text).group(1)


def scalar(text, name):
    return re.search(r"\b" + name + r" = (\w+);", text).group(1)


def switch(text, func):
    """a switch function's cases: [(codes, returned expression)], the default last with codes []"""
    m = re.search(re.escape(func) + r"\([^)]*\)\s*\{", text)
    end = text.index("\n}\n", m.end())
    body = text[m.end():end]
    out, codes = [], []
    for line in body.split("\n"):
        line = line.strip()
        for c in re.findall(r"case (0x[0-9A-Fa-f]+):", line):
            codes.append(int(c, 16))
        r = re.search(r"return (.*);", line)
        if r:
            out.append((codes, r.group(1)))
            codes = []
    return out


# (name in Go, header text, C array, element width, signed, fields per record, inner pad)
ucdArrays = [
    ("ctypeRecords", TYPE, "_PyUnicode_TypeRecords", 4, True, "upper lower title decimal digit flags", None),
    ("ctypeExtendedCase", TYPE, "_PyUnicode_ExtendedCase", 4, False, "", None),
    ("ctypeIndex1", TYPE, "index1", 2, False, "", None),
    ("ctypeIndex2", TYPE, "index2", 2, False, "", None),
    ("dbRecords", DATA, "_PyUnicode_Database_Records", 1, False,
     "category combining bidirectional mirrored east_asian_width normalization_quick_check", None),
    ("nfcFirst", DATA, "nfc_first", 4, False, "start count index", None),
    ("nfcLast", DATA, "nfc_last", 4, False, "start count index", None),
    ("dbIndex1", DATA, "index1", 2, False, "", None),
    ("dbIndex2", DATA, "index2", 2, False, "", None),
    ("decompData", DATA, "decomp_data", 4, False, "", None),
    ("decompIndex1", DATA, "decomp_index1", 1, False, "", None),
    ("decompIndex2", DATA, "decomp_index2", 2, False, "", None),
    ("compIndex", DATA, "comp_index", 2, False, "", None),
    ("compData", DATA, "comp_data", 4, False, "", None),
    ("changes320Index", DATA, "changes_3_2_0_index", 1, False, "", None),
    ("changes320Data", DATA, "changes_3_2_0_data", 1, False, "", None),
    ("packedNameDawg", NAME, "packed_name_dawg", 1, False, "", None),
    ("dawgPosToCodepoint", NAME, "dawg_pos_to_codepoint", 4, False, "", None),
    ("dawgCodepointToPosIndex1", NAME, "dawg_codepoint_to_pos_index1", 1, False, "", None),
    ("dawgCodepointToPosIndex2", NAME, "dawg_codepoint_to_pos_index2", 2, False, "", None),
    ("nameAliases", NAME, "name_aliases", 4, False, "", None),
    ("namedSequences", NAME, "named_sequences", 4, False, "seqlen seq0 seq1 seq2 seq3", 4),
    ("derivedNameRanges", NAME, "derived_name_ranges", 4, False, "first last prefixid", None),
]

# DATA's index1/index2 come after unicodedata_db.h's other arrays named index1: take the one after "#define SHIFT 7"
def data_ints(name):
    if name in ("index1", "index2"):
        return ints(DATA[DATA.index("#define SHIFT 7"):], name)
    return ints(DATA, name)


blob = bytearray()
counts = []
for go, text, c, width, signed, fields, pad in ucdArrays:
    vals = data_ints(c) if text is DATA else ints(text, c, pad=pad)
    nf = len(fields.split()) if fields else 1
    if len(vals) % nf:
        raise SystemExit("unicodedata_table: %s: %d values, not records of %d" % (c, len(vals), nf))
    fmt = {1: "b", 2: "h", 4: "i"}[width] if signed else {1: "B", 2: "H", 4: "I"}[width]
    nb = go.encode()
    blob += bytes([len(nb)]) + nb + bytes([width | (0x80 if signed else 0)]) + struct.pack("<I", len(vals))
    blob += struct.pack("<%d%s" % (len(vals), fmt), *vals)
    counts.append((go, c, len(vals), fields))

numeric = []
for codes, expr in switch(TYPE, "_PyUnicode_ToNumeric"):
    e = expr.replace("(double)", "").strip()
    a, _, b = e.partition("/")
    for code in codes:
        numeric.append("%X %s %s" % (code, a.strip(), b.strip() or "1"))
numeric.sort(key=lambda l: int(l.split()[0], 16))


def caselist(func):
    cs = switch(TYPE, func)
    assert cs[0][1] == "1" and cs[-1][1] == "0", func
    return " ".join("%X" % c for c in cs[0][0])


norm320 = []
for codes, expr in switch(DATA, "normalization_3_2_0"):
    for code in codes:
        norm320.append("%X %X" % (code, int(expr, 16)))

changes = []
for rec in re.findall(r"\{([^}]*)\}", array_body(DATA, "change_records_3_2_0")):
    changes.append(" ".join(v.strip() for v in rec.split(",")))

b64 = base64.b64encode(zlib.compress(bytes(blob), 9)).decode()


def goslice(name, vals):
    return "var %s = []string{%s}\n" % (name, ", ".join('"%s"' % v for v in vals))


o = []
o.append("// Code generated by tools/pyi/unicodedata_table.py from CPython 3.14.10's Objects/unicodetype_db.h,\n"
         "// Modules/unicodedata_db.h and Modules/unicodename_db.h; DO NOT EDIT.\n\n")
o.append("package py\n\n")
o.append("// The Unicode database's tables (PY6c): see tools/pyi/unicodedata_table.py for the format.\n\n")
consts = []
consts.append(("ucdVersion", define(DATA, "UNIDATA_VERSION")))
consts.append(("ctypeShift", TYPE[TYPE.index("#define SHIFT"):].split()[2]))
consts.append(("dbShift", DATA[DATA.index("#define SHIFT"):].split()[2]))
consts.append(("decompShift", define(DATA, "DECOMP_SHIFT")))
consts.append(("compShift", define(DATA, "COMP_SHIFT")))
consts.append(("totalFirst", define(DATA, "TOTAL_FIRST")))
consts.append(("totalLast", define(DATA, "TOTAL_LAST")))
consts.append(("nameMaxlen", define(NAME, "NAME_MAXLEN")))
consts.append(("dawgCodepointToPosShift", define(NAME, "DAWG_CODEPOINT_TO_POS_SHIFT")))
consts.append(("dawgCodepointToPosNotFound", define(NAME, "DAWG_CODEPOINT_TO_POS_NOTFOUND")))
for go, c in (("aliasesStart", "aliases_start"), ("aliasesEnd", "aliases_end"),
              ("namedSequencesStart", "named_sequences_start"), ("namedSequencesEnd", "named_sequences_end")):
    consts.append((go, scalar(NAME, c)))
w = max(len(n) for n, _ in consts)
o.append("const (\n" + "".join("\t%s = %s\n" % (n.ljust(w), v) for n, v in consts) + ")\n\n")
o.append(goslice("ucdCategoryNames", strings(DATA, "_PyUnicode_CategoryNames")))
o.append(goslice("ucdBidirectionalNames", strings(DATA, "_PyUnicode_BidirectionalNames")))
o.append(goslice("ucdEastAsianWidthNames", strings(DATA, "_PyUnicode_EastAsianWidthNames")))
o.append(goslice("ucdDecompPrefix", strings(DATA, "decomp_prefix")))
o.append(goslice("ucdDerivedNamePrefixes", strings(NAME, "derived_name_prefixes")))
o.append("\n// ucdArrayFields: each array of ucdBlob, its C name, its elements and its record's fields.\n")
o.append("const ucdArrayFields = `" + "\n".join("%s %s %d %s" % (g, c, n, f.replace(" ", ",") or "-")
                                             for g, c, n, f in counts) + "\n`\n\n")
o.append("// ucdNumeric is _PyUnicode_ToNumeric: CODE A B, the value (double) A/B.\n")
o.append("const ucdNumeric = `" + "\n".join(numeric) + "\n`\n\n")
o.append("// ucdWhitespace and ucdLinebreak are _PyUnicode_IsWhitespace's and _PyUnicode_IsLinebreak's cases.\n")
o.append("const ucdWhitespace = `" + caselist("_PyUnicode_IsWhitespace") + "`\n\n")
o.append("const ucdLinebreak = `" + caselist("_PyUnicode_IsLinebreak") + "`\n\n")
o.append("// ucdNormalization320 is normalization_3_2_0: CODE NORMALIZED.\n")
o.append("const ucdNormalization320 = `" + "\n".join(norm320) + "\n`\n\n")
o.append("// ucdChanges320 is change_records_3_2_0: bidir category decimal mirrored east_asian_width numeric.\n")
o.append("const ucdChanges320 = `" + "\n".join(changes) + "\n`\n\n")
o.append("// ucdBlob is the arrays (the format above).\n")
o.append('const ucdBlob = "' + b64 + '"\n')
text = "".join(o)

if check:
    have = open(out, encoding="utf-8").read()
    cut = lambda t: t[:t.index('const ucdBlob = "')]
    blob_of = lambda t: zlib.decompress(base64.b64decode(t[t.index('const ucdBlob = "') + 17:].split('"')[0]))
    if cut(have) != cut(text) or blob_of(have) != bytes(blob):
        print("unicodedata_table: %s differs from the tables of %s" % (out, src))
        sys.exit(1)
    print("unicodedata_table: %s equal to the tables of %s (%d arrays, %d bytes)" % (out, src, len(counts), len(blob)))
else:
    open(out, "w", encoding="utf-8").write(text)
    print("unicodedata_table: %s: %d arrays, %d bytes, %d compressed" % (out, len(counts), len(blob), len(b64)))

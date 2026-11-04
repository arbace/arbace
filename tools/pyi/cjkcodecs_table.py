# cjkcodecs_table.py PYTHON_SRC OUT.go [--check]: the CJK codecs' mapping tables (PY6c2), go/arbace/py's
# builtins_cjk_table.go, from CPython 3.14.10's own generated headers in the pinned tarball: Modules/cjkcodecs's
# mappings_cn.h, mappings_hk.h, mappings_jp.h, mappings_jisx0213_pair.h, mappings_kr.h and mappings_tw.h (made by
# CPython's Tools/unicode/genmap_*.py). The arrays are taken as the headers spell them, no table is recomputed, so
# the codecs' lookups (TRYMAP_ENC, TRYMAP_DEC, find_pairencmap) are the C code's over the same data.
#
# OUT.go holds one constant a header, cjkBlob<LOCALE> (CN HK JP JP2 KR TW; JP2 is mappings_jisx0213_pair.h), base64 of
# zlib (level 9) of the concatenation of records
#   KIND(1) NAME-LENGTH(1) NAME BASE-LENGTH(1) BASE WIDTH(1: 1, 2 or 4) COUNT(4, little-endian) ELEMENTS(little-endian)
# KIND 'd' a data array (ucs2_t, DBCHAR, Py_UCS4 or unsigned char; U, N, M and D as cjkcodecs.h defines them; BASE
# empty); KIND 'i' an index of 256 entries (struct dbcs_index, widedbcs_index, unim_index), flattened as
# (OFFSET+1 into the data array BASE, or 0 for a NULL map; bottom; top), width 4; KIND 's' a struct array flattened
# field by field (gb18030_to_unibmp_ranges: first last base; jisx0213_pair_encmap: uniseq code), width 4, BASE empty;
# and cjkPairCount, mappings_jisx0213_pair.h's JISX0213_ENCPAIRS. Deterministic for a given tarball; --check
# regenerates and requires OUT.go equal to it but for the blobs' text, compared decompressed (zlib's bytes may differ
# between zlib versions; the data may not).
# Run: python3 -I -S -B tools/pyi/cjkcodecs_table.py .tmp/repr-python/Python-3.14.10 OUT.go [--check]
import base64
import re
import struct
import sys
import zlib

src, out = sys.argv[1], sys.argv[2]
check = "--check" in sys.argv[3:]

HEADERS = [("CN", "mappings_cn.h"), ("HK", "mappings_hk.h"), ("JP", "mappings_jp.h"),
           ("JP2", "mappings_jisx0213_pair.h"), ("KR", "mappings_kr.h"), ("TW", "mappings_tw.h")]
MACROS = {"U": 0xFFFE, "N": 0xFFFF, "M": 0xFFFE, "D": 0xFFFD}
WIDTH = {"ucs2_t": 2, "DBCHAR": 2, "Py_UCS4": 4, "unsigned char": 1}
INDEX = ("struct dbcs_index", "struct widedbcs_index", "struct unim_index")
DECL = re.compile(r"^static const (ucs2_t|DBCHAR|Py_UCS4|unsigned char|struct \w+(?: \{[^}]*\})?) (\w+)\[(\w*)\] = \{",
                  re.M | re.S)


def body(text, start):
    """the initializer's text from start (just after its '{') to its closing brace, whitespace removed"""
    depth, j = 1, start
    while depth:
        c = text[j]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
        j += 1
    return re.sub(r"\s+", "", text[start:j - 1])


def num(tok):
    return MACROS[tok] if tok in MACROS else int(tok, 0)


def records(text, hname):
    """the header's arrays in order: (kind, name, base, width, values)"""
    out = []
    # the active branch of mappings_jisx0213_pair.h's #ifdef EXTERN_JISX0213_PAIR (the #else: the definitions)
    if "#ifdef EXTERN_JISX0213_PAIR" in text:
        text = text[text.index("#else"):text.index("#endif")]
    for m in DECL.finditer(text):
        ctype, name, size = m.group(1), m.group(2), m.group(3)
        b = body(text, m.end())
        if ctype in WIDTH:
            vals = [num(t) for t in b.rstrip(",").split(",")]
            if size and size.isdigit():
                assert len(vals) == int(size), (hname, name, len(vals), size)
            out.append(("d", name, "", WIDTH[ctype], vals))
        elif ctype in INDEX:
            ents = re.findall(r"\{([^{}]*)\}", b)
            assert len(ents) == 256 and size == "256", (hname, name, len(ents))
            base, vals = None, []
            for e in ents:
                p, lo, hi = e.split(",")
                if p == "0":
                    assert lo == "0" and hi == "0", (name, e)
                    vals += [0, 0, 0]
                    continue
                bm = re.fullmatch(r"(\w+)\+(\d+)", p)
                assert bm, (name, e)
                assert base in (None, bm.group(1)), (name, base, bm.group(1))
                base = bm.group(1)
                vals += [int(bm.group(2)) + 1, int(lo), int(hi)]
            out.append(("i", name, base or "", 4, vals))
        else:
            vals = [num(t) for e in re.findall(r"\{([^{}]*)\}", b) for t in e.split(",")]
            out.append(("s", name, "", 4, vals))
    return out


def pack(recs):
    blob = bytearray()
    for kind, name, base, width, vals in recs:
        blob += kind.encode() + bytes([len(name)]) + name.encode() + bytes([len(base)]) + base.encode()
        blob += bytes([width]) + struct.pack("<I", len(vals))
        fmt = {1: "B", 2: "H", 4: "I"}[width]
        blob += struct.pack("<%d%s" % (len(vals), fmt), *vals)
    return bytes(blob)


blobs, summary = [], []
pairs = None
for loc, h in HEADERS:
    text = open(src + "/Modules/cjkcodecs/" + h, encoding="utf-8").read()
    recs = records(text, h)
    if loc == "JP2":
        pairs = int(re.search(r"#define JISX0213_ENCPAIRS (\d+)", text).group(1))
        assert len([r for r in recs if r[1] == "jisx0213_pair_encmap"][0][4]) == 2 * pairs
    names = {r[1] for r in recs}
    for r in recs:
        assert r[0] != "i" or r[2] in names, (h, r[1], r[2])
    blobs.append((loc, h, pack(recs)))
    summary.append("%s %s %s" % (loc, h, " ".join("%s:%s:%d" % (r[0], r[1], len(r[4])) for r in recs)))

o = []
o.append("// Code generated by tools/pyi/cjkcodecs_table.py from CPython 3.14.10's Modules/cjkcodecs/mappings_*.h;\n"
         "// DO NOT EDIT.\n\n")
o.append("package py\n\n")
o.append("// The CJK codecs' mapping tables (PY6c2): see tools/pyi/cjkcodecs_table.py for the format.\n\n")
o.append("// cjkPairCount is mappings_jisx0213_pair.h's JISX0213_ENCPAIRS.\n")
o.append("const cjkPairCount = %d\n\n" % pairs)
o.append("// cjkTableArrays: each blob's header and its arrays (KIND:NAME:ELEMENTS).\n")
o.append("const cjkTableArrays = `" + "\n".join(summary) + "\n`\n")
for loc, h, blob in blobs:
    o.append("\n// cjkBlob%s is %s's arrays.\n" % (loc, h))
    o.append('const cjkBlob%s = "%s"\n' % (loc, base64.b64encode(zlib.compress(blob, 9)).decode()))
text = "".join(o)

if check:
    have = open(out, encoding="utf-8").read()
    pat = re.compile(r'const cjkBlob(\w+) = "([^"]*)"')
    cut = lambda t: pat.sub(lambda m: "const cjkBlob%s = \"\"" % m.group(1), t)
    got = {m.group(1): zlib.decompress(base64.b64decode(m.group(2))) for m in pat.finditer(have)}
    want = {loc: blob for loc, _, blob in blobs}
    if cut(have) != cut(text) or got != want:
        print("cjkcodecs_table: %s differs from the tables of %s" % (out, src))
        sys.exit(1)
    print("cjkcodecs_table: %s equal to the tables of %s (%d headers, %d arrays, %d bytes)" %
          (out, src, len(blobs), sum(len(s.split()) - 2 for s in summary), sum(len(b) for _, _, b in blobs)))
else:
    open(out, "w", encoding="utf-8").write(text)
    print("cjkcodecs_table: %s: %d headers, %d bytes, %d compressed" %
          (out, len(blobs), sum(len(b) for _, _, b in blobs), sum(len(base64.b64encode(zlib.compress(b, 9))) for _, _, b in blobs)))

#!/usr/bin/env python3
"""repr/python's token helper (PY9, doc/python/PYTHON-SURVEY.md "As built: PY9a"): the reference of the native lexer
(arbace.repr.python.lex), run as `python3 -I -S -B`. Each file is read as data (nothing imported or run).

usage:
  tokdump.py --base DIR --list FILE --out DIR [--tsv FILE]
      every file of the list (paths relative to DIR, one a line): its encoding found as tokenize.tokenize finds it
      (tokenize.detect_encoding: the BOM, PEP 263's cookie), then the token streams of CPython's C tokenizer
      (_tokenize.TokenizerIter over the file's lines, decoded with that encoding, as tokenize.tokenize runs it)
      in its two modes: with extra_tokens (tokenize's stream: COMMENT and NL, operators as OP) and without (the
      parser's: the exact operator types, the identifiers and the numbers' ends verified), written as OUT/REL.tok:
        @ENCODING                       the encoding (utf-8 for a BOM, as tokenize names it)
        #extra | #parser                the stream that follows
        TYPE SL SC EL EC STRING         one token a line, tab-separated: TYPE token.tok_name's, the positions as
                                        TokenizerIter gives them (lines from 1, columns in characters), STRING
                                        escaped (\\\\, \\n, \\r, \\t, \\uXXXX below U+0020 and U+007F)
        !CLASS L O EL EO MSG TEXT       the SyntaxError (or IndentationError, TabError) that ends a stream, its
                                        lineno, offset, end_lineno, end_offset (- for None), msg and text, escaped
      A file whose encoding cannot be found has one ! line (detect_encoding's SyntaxError) and no streams.
      The TSV, one line a file: REL STATUS TOKENS SECONDS (STATUS ok, or error when a stream ends in an error).
"""
import io
import os
import sys
import time
import tokenize
import warnings
import _tokenize

NAMES = tokenize.tok_name


def esc(s):
    out = []
    for ch in s:
        o = ord(ch)
        if ch == "\\":
            out.append("\\\\")
        elif ch == "\n":
            out.append("\\n")
        elif ch == "\r":
            out.append("\\r")
        elif ch == "\t":
            out.append("\\t")
        elif o < 0x20 or o == 0x7F:
            out.append("\\u%04X" % o)
        else:
            out.append(ch)
    return "".join(out)


def opt(x):
    return "-" if x is None else str(x)


def err_line(e):
    return "!%s\t%s\t%s\t%s\t%s\t%s\t%s" % (type(e).__name__, opt(getattr(e, "lineno", None)), opt(getattr(e, "offset", None)),
                                         opt(getattr(e, "end_lineno", None)), opt(getattr(e, "end_offset", None)),
                                         esc(str(getattr(e, "msg", None) or e)),
                                         "-" if getattr(e, "text", None) is None else esc(e.text))


def stream(data, encoding, consumed_n, extra, out):
    """TokenizerIter over the lines tokenize.tokenize gives it: detect_encoding's consumed lines (the BOM stripped),
    then the rest of BytesIO's lines"""
    rl = io.BytesIO(data)
    lines = []
    while True:
        l = rl.readline()
        if not l:
            break
        lines.append(l)
    if data.startswith(b"\xef\xbb\xbf") and lines:
        lines[0] = lines[0][3:]
        if not lines[0]:
            lines = lines[1:]
    it = iter(lines)
    n = 0
    try:
        for (typ, s, (sl, sc), (el, ec), _line) in _tokenize.TokenizerIter(it.__next__, extra_tokens=extra, encoding=encoding):
            out.append("%s\t%d\t%d\t%d\t%d\t%s" % (NAMES[typ], sl, sc, el, ec, esc(s)))
            n += 1
        return n, False
    except SyntaxError as e:
        out.append(err_line(e))
        return n, True


def one(path):
    data = open(path, "rb").read()
    out = []
    try:
        enc, consumed = tokenize.detect_encoding(io.BytesIO(data).readline)
    except SyntaxError as e:
        out.append(err_line(e))
        return out, 0, "error"
    if enc == "utf-8-sig":
        enc = "utf-8"
    out.append("@" + enc)
    total, bad = 0, False
    for name, extra in (("extra", True), ("parser", False)):
        out.append("#" + name)
        n, b = stream(data, enc, len(consumed), extra, out)
        total += n
        bad = bad or b
    return out, total, "error" if bad else "ok"


def main():
    a = sys.argv[1:]
    base = a[a.index("--base") + 1]
    lst = a[a.index("--list") + 1]
    outd = a[a.index("--out") + 1]
    tsv = open(a[a.index("--tsv") + 1], "w", encoding="utf-8") if "--tsv" in a else None
    warnings.simplefilter("ignore")
    for rel in [l for l in open(lst, encoding="utf-8").read().split("\n") if l]:
        t0 = time.perf_counter()
        try:
            lines, n, status = one(os.path.join(base, rel))
        except Exception as e:  # the helper's own failure, reported
            lines, n, status = ["!HelperError\t-\t-\t-\t-\t%s\t-" % esc("%s: %s" % (type(e).__name__, e))], 0, "helper-error"
        dst = os.path.join(outd, rel + ".tok")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(dst, "w", encoding="utf-8", errors="surrogatepass", newline="\n") as f:
            f.write("\n".join(lines) + "\n")
        if tsv:
            tsv.write("%s\t%s\t%d\t%.4f\n" % (rel, status, n, time.perf_counter() - t0))
    if tsv:
        tsv.close()


if __name__ == "__main__":
    main()

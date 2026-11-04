# PY6c: the Unicode database (unicodedata, UCD 16.0.0) and str's case mappings and predicates over it
import unicodedata as U

print(U.unidata_version, U.ucd_3_2_0.unidata_version, type(U.ucd_3_2_0).__name__)
for c in "Aé½²٣Ⅷ一각\U0001f600́ßﬁİẞǅ  ":
    print(hex(ord(c)), U.category(c), U.bidirectional(c), U.combining(c), U.mirrored(c), U.east_asian_width(c),
          repr(U.decomposition(c)), U.name(c, None), U.decimal(c, None), U.digit(c, None), U.numeric(c, None),
          U.ucd_3_2_0.category(c), U.ucd_3_2_0.name(c, None))
for name in ["LATIN SMALL LETTER A", "latin small letter a", "HANGUL SYLLABLE GAG", "CJK UNIFIED IDEOGRAPH-4E00",
             "LATIN CAPITAL LETTER GHA", "KEYCAP NUMBER SIGN", "NULL"]:
    try:
        print(name, [hex(ord(x)) for x in U.lookup(name)])
    except KeyError as e:
        print(name, "KeyError", e)
s = "ẛ̣ Å ﬁ ḱṷṓn 각 Å"
for f in ("NFC", "NFD", "NFKC", "NFKD"):
    n = U.normalize(f, s)
    print(f, [hex(ord(x)) for x in n], U.is_normalized(f, s), U.is_normalized(f, n))
for bad in [(U.category, "ab"), (U.category, 3), (U.name, "\U000e0000"), (U.decimal, "x"), (U.numeric, "x")]:
    try:
        print(bad[0](bad[1]))
    except (TypeError, ValueError) as e:
        print(type(e).__name__, e)
for w in ["ΣΑΣ", "ὈΔΥΣΣΕΎΣ", "straße", "ﬁnal", "İstanbul", "ǅemal", "hello world", "aΣ b", "ΣΣ", "ǉ x"]:
    print(w.upper(), w.lower(), w.casefold(), w.swapcase(), w.capitalize(), w.title())
for w in ["²", "١٢٣", "Ⅷ", "½", "abc", "ABC", "Abc Def", "ǅ", " ", "\x1c", " \t", "x́", "_a1", "1a", "a·",
          " ", "﻿", "ǅa", ""]:
    print(repr(w), [m for m in ("isalpha", "isdecimal", "isdigit", "isnumeric", "isalnum", "isspace", "isprintable",
                                "isupper", "islower", "istitle", "isidentifier") if getattr(w, m)()])
print(repr("͸­⁦\U000e0001x​"))
print("a b\x1cc\x85d".splitlines())

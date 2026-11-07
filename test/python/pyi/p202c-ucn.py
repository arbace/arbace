# queue 202c: \N{NAME} escapes as CPython's parser resolves them (unicodedata.c's _getcode over CPython's tables):
# aliases, Hangul syllables, derived CJK and Tangut names, case folding; named sequences and unknown names refused.
import ast

names = ["RS", "LINE FEED", "lf", "BYTE ORDER MARK", "ZWJ", "latin small letter a", "LATIN CAPITAL LETTER A",
         "HANGUL SYLLABLE GA", "hangul syllable gyeoss", "HANGUL SYLLABLE GGX", "CJK UNIFIED IDEOGRAPH-4E00",
         "cjk unified ideograph-9fff", "CJK UNIFIED IDEOGRAPH-04E00", "CJK UNIFIED IDEOGRAPH-FFFF",
         "TANGUT IDEOGRAPH-17000", "TANGUT IDEOGRAPH-18D08", "BASIC LATIN 41", " LATIN SMALL LETTER A",
         "LATIN CAPITAL LETTER A WITH MACRON AND GRAVE", "KEYCAP NUMBER SIGN", "NO SUCH NAME", "X" * 300,
         "PILE OF POO", "BELL", "BEL", "ALERT", "NULL", "END OF MEDIUM", "SPACE", "EGYPTIAN HIEROGLYPH A001"]
for n in names:
    src = '"\\N{%s}"' % n
    try:
        v = eval(src)
        print(repr(n[:40]), "->", ascii(v))
    except SyntaxError as e:
        print(repr(n[:40]), "SyntaxError:", e.msg, e.lineno, e.offset)
    try:
        t = ast.parse(src, mode="eval")
        print("  ast", ascii(t.body.value))
    except SyntaxError as e:
        print("  ast SyntaxError:", e.msg)
print(ascii("\N{RS}\N{LINE FEED}\N{hangul syllable ga}"))
print(ascii(f"{1}\N{RS}"))

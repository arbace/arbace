# PY6c: _codecs' codecs and error handlers, the Unicode errors' str and checks, the encodings package over them
import codecs, _codecs, warnings

def show(f, *a):
    with warnings.catch_warnings(record=True) as w:
        warnings.simplefilter("always")
        try:
            r = f(*a)
            print(repr(r))
        except Exception as e:
            print(type(e).__name__, str(e), getattr(e, "__notes__", None))
    for x in w:
        print("warning", x.category.__name__, x.message)

s = "a\xe9€\U0001f600\udc80z"
for enc in ["utf-8", "utf-16", "utf-16-le", "utf-16-be", "utf-32", "utf-32-le", "utf-32-be", "utf-7", "latin-1", "ascii",
            "cp437", "cp1252", "mac-roman", "iso8859-15", "koi8-r", "cp037", "unicode_escape", "raw_unicode_escape"]:
    for errors in ["strict", "ignore", "replace", "backslashreplace", "xmlcharrefreplace", "namereplace", "surrogateescape",
                   "surrogatepass"]:
        show(s.encode, enc, errors)
data = b"a\xc3\xa9\xff\xed\xa0\x80\xe2\x82z\x80"
for enc in ["utf-8", "utf-16-le", "utf-32-be", "utf-7", "ascii", "latin-1", "cp437", "cp1252", "unicode_escape", "utf-8-sig"]:
    for errors in ["strict", "ignore", "replace", "backslashreplace", "surrogateescape", "surrogatepass"]:
        show(data.decode, enc, errors)
for b in [b"+AGEAYgBj-", b"+2D3eAA-", b"a+-b", b"+AGE", b"+Ab-", b"\x80", b"+!"]:
    show(codecs.utf_7_decode, b, "replace", True)
    show(codecs.utf_7_decode, b, "strict", False)
for b in [b"\\x41\\u20ac\\U0001f600\\N{EURO SIGN}\\101\\q", b"\\x4", b"\\N{NOPE}", b"abc\\", b"\\u12"]:
    show(codecs.unicode_escape_decode, b, "replace")
    show(codecs.unicode_escape_decode, b, "strict", False)
    show(codecs.raw_unicode_escape_decode, b, "backslashreplace")
    show(codecs.escape_decode, b)
show(codecs.escape_encode, b"a'\\\t\n\r\x00\x7f\xff")
for b in [b"\xff\xfea\x00\x00\xd8", b"\xfe\xff\x00a\xdc\x00", b"a\x00\x00", b"\x00\xd8\x00\xdc"]:
    show(codecs.utf_16_ex_decode, b, "replace", 0, True)
    show(codecs.utf_16_decode, b, "strict", False)
for b in [b"\xff\xfe\x00\x00a\x00\x00\x00\x00\xd8\x00\x00", b"\x00\x00\x11\x00", b"ab"]:
    show(codecs.utf_32_ex_decode, b, "replace", 0, True)
    show(codecs.utf_32_decode, b, "strict", False)
show(codecs.charmap_decode, b"\x00\x01\x02", "replace", "ab")
show(codecs.charmap_decode, b"\x00\x01\x02", "strict", {0: "x", 1: 0x41, 2: None})
show(codecs.charmap_encode, "abc", "replace", {ord("a"): 1, ord("b"): b"BB", ord("?"): 63})
m = codecs.charmap_build("".join(chr(i) for i in range(256)))
print(type(m).__name__, m.size())
show(codecs.charmap_encode, "aĀ", "xmlcharrefreplace", m)
for name in ["UTF8", "utf_8", "Latin-1", "  utf--8  ", "IDNA", "punycode", "cp-1252", "nope"]:
    try:
        print(name, codecs.lookup(name).name)
    except LookupError as e:
        print(name, "LookupError", e)
show("bücher.example".encode, "idna")
show(b"xn--bcher-kva.example".decode, "idna")
show("bücher".encode, "punycode")
show(b"bcher-kva".decode, "punycode")
show("abc".encode, "rot13")
show(codecs.encode, "abc", "rot13")
for e in [UnicodeEncodeError("ascii", "a\xe9b", 1, 2, "bad"), UnicodeEncodeError("ascii", "abc", 0, 3, "bad"),
          UnicodeDecodeError("utf-8", b"a\xffb", 1, 2, "bad"), UnicodeDecodeError("utf-8", bytearray(b"ab"), 0, 5, "x"),
          UnicodeTranslateError("a\U0001f600", 1, 2, "bad"), UnicodeEncodeError("ascii", "€", 0, 1, "r")]:
    print(str(e), e.start, e.end, e.reason, repr(e.object), e.args)
    e.start = 0
    e.reason = "changed"
    print(str(e))
for args in [("ascii", b"x", 0, 1, "r"), ("ascii", "x", "0", 1, "r"), ("ascii",), (1, "x", 0, 1, "r")]:
    show(UnicodeEncodeError, *args)
e = UnicodeEncodeError("ascii", "abc", 0, 1, "r")
try:
    e.start = "x"
except TypeError as x:
    print("TypeError", x)
try:
    del e.end
except TypeError as x:
    print("TypeError", x)
e.object = None
show(str, e)
print(str(UnicodeEncodeError.__new__(UnicodeEncodeError)), repr(UnicodeError("x")))
def handler(exc):
    print("handler", type(exc).__name__, exc.start, exc.end, exc.reason)
    return ("<%d>" % exc.start, exc.end)
codecs.register_error("test.c", handler)
show("a\xe9\xe9b".encode, "ascii", "test.c")
show(b"a\xff\xfeb".decode, "utf-8", "test.c")
show("a\xe9".encode, "cp437", "test.c")
print(codecs.lookup_error("strict"), codecs.lookup_error("test.c") is handler)
show(codecs.lookup_error, "nope")
print(_codecs._unregister_error("test.c"), _codecs._unregister_error("test.c"))
show(_codecs._unregister_error, "strict")
for h in ["strict", "ignore", "replace", "backslashreplace", "xmlcharrefreplace", "namereplace", "surrogateescape", "surrogatepass"]:
    show(codecs.lookup_error(h), UnicodeEncodeError("utf-8", "a\udc80€b", 1, 3, "r"))
    show(codecs.lookup_error(h), UnicodeDecodeError("utf-8", b"a\xed\xa0\x80b", 1, 4, "r"))
    show(codecs.lookup_error(h), UnicodeTranslateError("a€b", 1, 2, "r"))
    show(codecs.lookup_error(h), ValueError("x"))
dec = codecs.getincrementaldecoder("utf-16")()
print([dec.decode(bytes([c])) for c in b"\xff\xfea\x00\x00\xd8\x00\xdc"])
dec = codecs.getincrementaldecoder("utf-8")()
print([dec.decode(bytes([c])) for c in "a€\U0001f600".encode()], dec.decode(b"", True))
enc = codecs.getincrementalencoder("utf-16")()
print(enc.encode("a"), enc.encode("b"))
show(b"\xff".decode, "utf-8")
show(b"abc".decode, "utf-16")
show("\udc80".encode)
import io
w = io.TextIOWrapper(io.BytesIO(), encoding="cp1252")
w.write("caf\xe9 €")
w.flush()
print(w.buffer.getvalue())

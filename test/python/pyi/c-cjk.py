# PY6c2: the CJK codecs (_codecs_cn, _tw, _hk, _jp, _kr, _iso2022) through _multibytecodec: every codec's encode and
# decode with the error handlers, custom handlers, the incremental encoders' and decoders' state, the stream classes
import codecs, io, _multibytecodec

def show(f, *a):
    try:
        r = f(*a)
        print(repr(r))
    except Exception as e:
        print(type(e).__name__, str(e))

ALL = ["gb2312", "gbk", "gb18030", "hz", "big5", "cp950", "big5hkscs", "shift_jis", "cp932", "euc_jp", "shift_jis_2004",
       "euc_jis_2004", "euc_jisx0213", "shift_jisx0213", "euc_kr", "cp949", "johab", "iso2022_kr", "iso2022_jp",
       "iso2022_jp_1", "iso2022_jp_2", "iso2022_jp_2004", "iso2022_jp_3", "iso2022_jp_ext"]
s = "aé世界あ가ｶ~\\Ê̄æ̀\U00020000\U0001f600\udc80z"
for enc in ALL:
    for errors in ["strict", "ignore", "replace", "backslashreplace", "xmlcharrefreplace", "namereplace", "surrogateescape"]:
        show(s.encode, enc, errors)
    e = s.encode(enc, "replace")
    for errors in ["strict", "ignore", "replace", "backslashreplace", "surrogateescape"]:
        show(e.decode, enc, errors)
        show((e[:-3] + b"\xff\x80\xa1").decode, enc, errors)
        show(e[:len(e) // 2 + 1].decode, enc, errors)

def rec(exc):
    print("handler", type(exc).__name__, exc.encoding, exc.start, exc.end, exc.reason, repr(exc.object[exc.start:exc.end]))
    if isinstance(exc, UnicodeEncodeError):
        return ("<%d>" % exc.start, exc.end)
    return ("[%d]" % exc.start, exc.end)
codecs.register_error("t.rec", rec)
codecs.register_error("t.bytes", lambda exc: (b"!!", exc.end))
def back(exc):
    back.n += 1
    return ("X", 0) if back.n < 3 else ("Y", exc.end)
codecs.register_error("t.back", back)
codecs.register_error("t.bad", lambda exc: ("x", "y"))
codecs.register_error("t.far", lambda exc: ("x", 10**30))
codecs.register_error("t.neg", lambda exc: ("x", -1 if exc.start < len(exc.object) - 1 else exc.end))
for enc in ["gbk", "big5", "euc_jp", "cp949", "iso2022_jp", "hz", "gb18030", "big5hkscs"]:
    back.n = 0
    for h in ["t.rec", "t.bytes", "t.back", "t.bad", "t.far", "t.neg"]:
        show("ab\udc80cĂd".encode, enc, h)
        show(b"ab\x80\xffc\xfe".decode, enc, h)
print(str(UnicodeDecodeError("gbk", b"\x81", 0, 1, "x")))

for enc in ["euc_jis_2004", "iso2022_jp", "hz", "gb18030", "big5hkscs", "iso2022_kr"]:
    ie = codecs.getincrementalencoder(enc)("backslashreplace")
    out = []
    for ch in "aÊæ世b가̄̀":
        out.append(ie.encode(ch))
        out.append(ie.getstate())
    out.append(ie.encode("", True))
    print(enc, out)
    ie.reset()
    print(ie.getstate(), ie.errors)
    ie.errors = "replace"
    print(ie.encode("\U0010ffffæ", True), ie.errors)
    idc = codecs.getincrementaldecoder(enc)()
    data = "x世界æ̀y".encode(enc, "ignore")
    out = []
    for i in range(len(data)):
        out.append(idc.decode(data[i:i + 1]))
        out.append(idc.getstate())
    out.append(idc.decode(b"", True))
    print(enc, out)
    show(idc.decode, data[:1] + b"\x8e", True)
    idc.setstate((b"\xa4", 66))
    print(idc.getstate())
    show(idc.setstate, (b"123456789", 0))
    show(idc.setstate, ("a", 0))
    show(idc.setstate, 5)
    show(ie.setstate, -1)
    show(ie.setstate, 9)
    show(ie.setstate, 1 << 200)

b = io.BytesIO("一行目\n二行目\nthird\n".encode("big5"))
r = codecs.getreader("big5")(b)
print(r.readline(), r.read(1), r.readlines(), r.read(), r.stream is b)
w = codecs.getwriter("iso2022_jp")(io.BytesIO())
w.write("abc世")
w.writelines(["x", "界"])
w.reset()
print(w.stream.getvalue())
show(_multibytecodec.MultibyteStreamReader, None)
show(_multibytecodec.MultibyteIncrementalEncoder)
show(codecs.lookup("big5").decode, "abc")
show(codecs.lookup("big5").encode, 5)
for name in ["_codecs_cn", "_codecs_tw", "_codecs_hk", "_codecs_jp", "_codecs_kr", "_codecs_iso2022"]:
    m = __import__(name)
    print(name, sorted(k for k in dir(m) if k.startswith("__map_")))
    show(m.getcodec, "nope")
    show(m.getcodec, 5)
print(type(codecs.lookup("cp932").incrementalencoder.codec).__name__, _multibytecodec.MultibyteIncrementalDecoder.__module__)
print("か゚".encode("euc_jis_2004"), "か゚".encode("shift_jisx0213").decode("shift_jisx0213"))
print(b"\x1b(B:hu4:unit\x1b.A\x1bNi de famille".decode("iso2022-jp-2"))

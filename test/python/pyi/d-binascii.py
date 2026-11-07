# PY6d: binascii (uu, base64 with strict_mode, hex with separators, qp, crc_hqx, crc32) and its messages
import binascii, base64, array
data = bytes(range(256)) * 2 + b"The quick brown fox\r\n.\n=x_y \t\n"
for f, g in [(binascii.b2a_base64, binascii.a2b_base64), (binascii.b2a_hex, binascii.a2b_hex), (binascii.b2a_qp, binascii.a2b_qp)]:
    e = f(data); print(f.__name__, len(e), e[:60], g(e) == data)
print(binascii.b2a_uu(data[:45]), binascii.a2b_uu(binascii.b2a_uu(data[:45], backtick=True)) == data[:45])
for sep, n in (("", 1), (":", 1), (b"_", 2), ("-", -3), (".", 8)):
    print(binascii.hexlify(data[:13], sep, n) if sep else binascii.hexlify(data[:13]), bytes(data[:13]).hex(sep, n) if sep else "")
print(binascii.crc32(data), binascii.crc32(data, 12345), binascii.crc_hqx(data, 0xffff), binascii.crc32(array.array("i", [1, 2, 3])))
print(binascii.b2a_qp(b"a b\t\nc.\n" + b"x" * 100, quotetabs=True, header=True), binascii.a2b_qp(b"a_b=3D=\r\nc", header=True))
print(base64.b32encode(b"hello"), base64.b85encode(b"hello"), base64.urlsafe_b64encode(b"\xff\xfe"), base64.b64decode(b"aGk=\n"))
for bad in (lambda: binascii.a2b_base64(b"abc", strict_mode=True), lambda: binascii.a2b_base64(b"=abc", strict_mode=True),
            lambda: binascii.a2b_base64(b"ab=c", strict_mode=True), lambda: binascii.a2b_base64(b"abcde"), lambda: binascii.a2b_hex(b"abc"),
            lambda: binascii.a2b_hex(b"zz"), lambda: binascii.a2b_uu(b"\x7f"), lambda: binascii.b2a_uu(b"x" * 46), lambda: binascii.a2b_hex("é"),
            lambda: binascii.b2a_hex("str"), lambda: binascii.hexlify(b"ab", "xy"), lambda: binascii.b2a_hex(memoryview(bytearray(b"abcd"))[::2])):
    try: bad()
    except Exception as e: print(type(e).__name__, e)
print(issubclass(binascii.Error, ValueError), binascii.Incomplete.__mro__[1].__name__)

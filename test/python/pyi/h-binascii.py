# PY6h: binascii's stand-in (base64's strict and non-strict rules, hex, crc32, crc_hqx)
import binascii, base64
for d in [b"", b"a", b"ab", b"abc", b"hello world\x00\xff"]:
    e = binascii.b2a_base64(d)
    print(e, binascii.a2b_base64(e), binascii.b2a_base64(d, newline=False), binascii.hexlify(d), binascii.crc32(d), binascii.crc_hqx(d, 0))
print(binascii.a2b_base64(b"aGVs\nbG8=!!"), binascii.a2b_base64("aGVsbA"+"=="), binascii.hexlify(b"abcdef", "-", 2), binascii.hexlify(b"abcde", b":", -2))
for bad in [b"a", b"aGV", b"aGVsb"]:
    try: binascii.a2b_base64(bad)
    except binascii.Error as e: print("Error", e)
for bad in [b"=aGVs", b"aG=Vs", b"aGVs!", b"aGU=x"]:
    try: print(binascii.a2b_base64(bad, strict_mode=True))
    except binascii.Error as e: print("Error", e)
print(binascii.unhexlify("4142"), binascii.crc32(b"x", -1), base64.b64encode(b"xyz"), base64.urlsafe_b64decode(b"eHl6"))
try: binascii.unhexlify("414")
except binascii.Error as e: print("Error", e)

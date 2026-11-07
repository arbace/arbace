# idna (test/python/wheels.edn): IDNA 2008 encoding and decoding, UTS 46 mapping, errors
import idna

for name in ["example.com", "bücher.de", "日本語.jp", "xn--nxasmq6b.com", "Straße.de"]:
    try:
        a = idna.encode(name, uts46=True)
        print(name, a, idna.decode(a))
    except idna.IDNAError as e:
        print(name, "IDNAError", e)
print(idna.alabel("ドメイン"), idna.ulabel(b"xn--eckwd4c7c"))
for bad in ["a..b", "-abc.com", "x" * 64 + ".com"]:
    try:
        idna.encode(bad)
        print(bad, "accepted")
    except idna.IDNAError as e:
        print(bad[:20], type(e).__name__, e)
print("bücher".encode("idna"), idna.uts46_remap("ÖBB.at"))

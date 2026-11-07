# _ssl's fallback store (queue 209, the user's decision (a) on DECISION 1, 2026-11-05): with no system store, the
# default verify paths take x509roots/'s bundle (Mozilla's roots, their distrust-after dates as constraints). pyi only:
# CPython has no fallback. Run as bin/pyi run test/python/pyi/ssl/fallback.py; it points SSL_CERT_FILE and SSL_CERT_DIR
# at an empty directory itself. Part 2 stands the test CA in for the bundle through the hook PYI_SSL_FALLBACK_ROOTS
# (no bundle root signs the test leaf), first unconstrained, then with a distrust-after date before the leaf.
import os, ssl, _ssl, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
empty = tempfile.mkdtemp()
os.environ["SSL_CERT_FILE"] = os.path.join(empty, "cert.pem")
os.environ["SSL_CERT_DIR"] = empty
os.environ.pop("PYI_SSL_FALLBACK_ROOTS", None)

print("1. the bundle, no system store")
ctx = ssl.create_default_context()
st = ctx.cert_store_stats()
print("roots", st["x509"], "CAs", st["x509_ca"], "crl", st["crl"])
roots = _ssl._x509roots()
print("bundle", len(roots), "roots,", sum(1 for _, d in roots if d), "with a distrust-after date")
for subj, date in roots:
    if date:
        print("e.g.", dict(a for rdn in subj for a in rdn).get("commonName"), "distrusted after", date)
        break
with_store = tempfile.mkdtemp()  # a store's file (and, in part 2, the hook's file: not in the empty store)
with open(os.path.join(with_store, "cert.pem"), "w") as f:
    f.write(open(os.path.join(HERE, "ca.pem")).read())
os.environ["SSL_CERT_FILE"] = os.path.join(with_store, "cert.pem")
print("with a system store:", ssl.create_default_context().cert_store_stats())
os.environ["SSL_CERT_FILE"] = os.path.join(empty, "cert.pem")

def handshake(client):
    server = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    server.load_cert_chain(os.path.join(HERE, "keycert.pem"))
    bios = [ssl.MemoryBIO() for _ in range(4)]
    c = client.wrap_bio(bios[0], bios[1], server_hostname="localhost")
    s = server.wrap_bio(bios[2], bios[3], server_side=True)
    done = [False, False]
    for _ in range(10):
        for i, o in enumerate((c, s)):
            if not done[i]:
                try:
                    o.do_handshake(); done[i] = True
                except ssl.SSLWantReadError:
                    pass
        if bios[1].pending: bios[2].write(bios[1].read())
        if bios[3].pending: bios[0].write(bios[3].read())
        if all(done):
            return c.version(), c.getpeercert()["subject"][-1]

print("2. the test leaf through the fallback (the hook)")
ca = open(os.path.join(HERE, "ca.pem")).read()
for header in ("", "Distrust-After: 2100-01-01T00:00:00Z\n\n", "Distrust-After: 2020-01-01T00:00:00Z\n\n"):
    hook = os.path.join(with_store, "roots.pem")
    with open(hook, "w") as f:
        f.write(ca.replace("-----BEGIN CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" + header))
    os.environ["PYI_SSL_FALLBACK_ROOTS"] = hook
    client = ssl.create_default_context()
    print(header.strip() or "unconstrained", "->", _ssl._x509roots()[0][1], "CAs", client.cert_store_stats()["x509_ca"])
    try:
        print("  ok", handshake(client))
    except ssl.SSLCertVerificationError as e:
        print("  ", type(e).__name__, e.verify_code, e.verify_message)
print("3. a system store again: the fallback (the hook, still distrusting) not consulted")
os.environ["SSL_CERT_FILE"] = os.path.join(with_store, "cert.pem")
print("  ok", handshake(ssl.create_default_context()))

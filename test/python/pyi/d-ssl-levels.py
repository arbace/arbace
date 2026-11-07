# _ssl after queue 217's wider sample: the security level (set_ciphers' "@SECLEVEL=N", load_cert_chain's key
# checks, verification's codes 66-68), a PEM block OpenSSL cannot read, the alert each verification failure sends
# under TLS 1.2, the automatic chain, the sessions before and after a ticket, OP_NO_TICKET under TLS 1.3.
# Left out (by design, the survey's "As built: 217"): TLS 1.3's alerts under the keys, tls-unique under TLS 1.3,
# SHA-1 at level 0.
import os, ssl

HERE = os.path.dirname(os.path.abspath(__file__))
CA = os.path.join(HERE, "ssl", "ca.pem")
CERT = os.path.join(HERE, "ssl", "keycert.pem")
L = os.path.join(HERE, "ssl", "levels") + os.sep

def pair(c, s, host="localhost"):
    ci, co, si, so = ssl.MemoryBIO(), ssl.MemoryBIO(), ssl.MemoryBIO(), ssl.MemoryBIO()
    a = c.wrap_bio(ci, co, server_hostname=host)
    b = s.wrap_bio(si, so, server_side=True)
    err, done = [None, None], [False, False]
    for _ in range(12):
        for i, x in enumerate((a, b)):
            if done[i] or err[i]:
                continue
            try:
                x.do_handshake()
                done[i] = True
            except ssl.SSLWantReadError:
                pass
            except ssl.SSLError as e:
                err[i] = "%s %s" % (type(e).__name__, e.reason)
                if isinstance(e, ssl.SSLCertVerificationError):
                    err[i] += " %d %s" % (e.verify_code, e.verify_message)
        if co.pending:
            si.write(co.read())
        if so.pending:
            ci.write(so.read())
    return a, b, err

def ctx(server, level=""):
    x = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER if server else ssl.PROTOCOL_TLS_CLIENT)
    if level:
        x.set_ciphers("DEFAULT" + level)
    return x

print("set_ciphers")
for spec in ("DEFAULT@SECLEVEL=1", "@SECLEVEL=0:DEFAULT", "DEFAULT:@SECLEVEL=3", "HIGH@STRENGTH@SECLEVEL=1"):
    x = ctx(False)
    try:
        x.set_ciphers(spec)
        print(spec, x.security_level, len(x.get_ciphers()) > 0)
    except ssl.SSLError as e:
        print(spec, e.args, x.security_level)

print("load_cert_chain")
for level in ("", "@SECLEVEL=1", "@SECLEVEL=3"):
    for f in ("weak-leaf.pem", "weak-ca-leaf.pem", "weak-chain.pem", "sha1-leaf.pem", "bad-block.pem"):
        x = ctx(True, level)
        try:
            x.load_cert_chain(L + f)
            print(level, f, "loaded")
        except ssl.SSLError as e:
            print(level, f, e.args, e.library, e.reason)

print("verification")
cases = [("unknown ca", L + "ca.pem", CERT, "localhost"), ("mismatch", CA, CERT, "example.org"),
         ("ip mismatch", CA, CERT, "192.0.2.1"), ("weak ca", L + "weak-ca.pem", L + "weak-ca-leaf.pem", "localhost"),
         ("weak leaf", L + "ca.pem", L + "weak-leaf.pem", "localhost"),
         ("sha1", L + "ca.pem", L + "sha1-leaf.pem", "localhost"), ("self-signed", CA, L + "self-signed.pem", "localhost"),
         ("expired", L + "expired-ca.pem", L + "expired.pem", "localhost")]
for name, ca, cert, host in cases:
    for level in ("", "@SECLEVEL=1"):
        c = ctx(False, level)
        c.load_verify_locations(ca)
        c.maximum_version = ssl.TLSVersion.TLSv1_2
        s = ctx(True, "@SECLEVEL=0")
        s.load_cert_chain(cert)
        a, b, e = pair(c, s, host)
        print(name, level, e)
c = ctx(False)
c.load_verify_locations(CA)
c.maximum_version = ssl.TLSVersion.TLSv1_2
c.load_cert_chain(L + "self-signed.pem")
s = ctx(True)
s.load_cert_chain(CERT)
s.verify_mode = ssl.CERT_REQUIRED
s.load_verify_locations(CA)
print("client certificate", pair(c, s)[2])

print("chains")
for give in (False, True):
    c = ctx(False)
    c.load_verify_locations(CA)
    s = ctx(True)
    s.load_cert_chain(CERT)
    s.load_verify_locations(CA)
    s.verify_mode = ssl.CERT_OPTIONAL
    if give:
        c.load_cert_chain(CERT)
    a, b, e = pair(c, s)
    print(give, e, len(a.get_unverified_chain()), len(a.get_verified_chain()),
          b.get_unverified_chain() and len(b.get_unverified_chain()))

print("sessions")
for v in (ssl.TLSVersion.TLSv1_2, ssl.TLSVersion.TLSv1_3):
    for notick in (False, True):
        c = ctx(False)
        c.load_verify_locations(CA)
        c.maximum_version = v
        s = ctx(True)
        s.load_cert_chain(CERT)
        if notick:
            s.options |= ssl.OP_NO_TICKET
        a, b, e = pair(c, s)
        p = a.session
        print(v.name, notick, "before", p.has_ticket, len(p.id), p.ticket_lifetime_hint, b.session.has_ticket, len(b.session.id))
        try:
            a.read(1)
        except ssl.SSLWantReadError:
            pass
        print(" after", a.session.has_ticket, len(a.session.id), a.session.ticket_lifetime_hint)

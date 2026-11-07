# _ssl over Go's crypto/tls (queue 209): a client and a server in one process, over MemoryBIOs and over a
# socket pair, with the test certificate of ssl/ (a CA and a leaf for localhost); printed as CPython prints it.
# The cipher suite each side picks is left out (Go's and OpenSSL's preferences differ: by design).
import os, socket, ssl, threading, pprint

HERE = os.path.dirname(os.path.abspath(__file__))
CA = os.path.join(HERE, "ssl", "ca.pem")
CERT = os.path.join(HERE, "ssl", "keycert.pem")

print("decode")
pprint.pprint(ssl._ssl._test_decode_cert(CERT))

def contexts(ca=True):
    client = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    if ca:
        client.load_verify_locations(CA)
    server = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    server.load_cert_chain(CERT)
    return client, server

def bio_pair(client_ctx, server_ctx, hostname="localhost"):
    c_in, c_out, s_in, s_out = ssl.MemoryBIO(), ssl.MemoryBIO(), ssl.MemoryBIO(), ssl.MemoryBIO()
    c = client_ctx.wrap_bio(c_in, c_out, server_hostname=hostname)
    s = server_ctx.wrap_bio(s_in, s_out, server_side=True)
    return c, s, c_in, c_out, s_in, s_out

def pump(c, s, c_in, c_out, s_in, s_out):
    done = [False, False]
    for _ in range(10):
        for i, (obj, src, dst) in enumerate(((c, s_out, c_in), (s, c_out, s_in))):
            if not done[i]:
                try:
                    obj.do_handshake()
                    done[i] = True
                except ssl.SSLWantReadError:
                    pass
        if c_out.pending:
            s_in.write(c_out.read())
        if s_out.pending:
            c_in.write(s_out.read())
        if all(done):
            return True
    return False

print("bio")
cc, sc = contexts()
c, s, c_in, c_out, s_in, s_out = bio_pair(cc, sc)
print(c.server_side, s.server_side, c.server_hostname, c.version(), c.cipher())
try:
    c.getpeercert()
except ValueError as e:
    print("ValueError", e)
try:
    c.read(10)
except ssl.SSLWantReadError as e:
    print("want read", e.args[0], type(e).__mro__[1].__name__)
print(pump(c, s, c_in, c_out, s_in, s_out))
print(c.version(), s.version(), c.cipher()[1], s.cipher()[1])
pprint.pprint(c.getpeercert())
print(s.getpeercert(), len(c.getpeercert(True)))
print([len(der) for der in c.get_verified_chain()], c._sslobj.get_verified_chain()[0].get_info()["subject"])
print(len(c.get_unverified_chain()))
print(c.write(b"hello over bio"))
s_in.write(c_out.read())
print(s.read(100), s.pending())
s.write(b"x" * 20000)
c_in.write(s_out.read())
got = b""
while len(got) < 20000:
    got += c.read(7000)
print(len(got), c.pending())
try:
    c.unwrap()
except ssl.SSLWantReadError:
    print("unwrap wants the peer's close_notify")
s_in.write(c_out.read())
try:
    s.unwrap()
except ssl.SSLWantReadError:
    print("server unwrap?")
c_in.write(s_out.read())
c.unwrap()
print("unwrapped", c_out.pending, s_out.pending)

print("verify")
for ca, host in ((False, "localhost"), (True, "otherhost"), (True, "127.0.0.1"), (True, "127.0.0.2")):
    cc, sc = contexts(ca)
    c, s, *bios = bio_pair(cc, sc, host)
    try:
        pump(c, s, *bios)
        print(host, "ok", c.version())
    except ssl.SSLCertVerificationError as e:
        print(host, type(e).__name__, e.verify_code, e.verify_message, e.reason, e.library)
        print(str(e).split(" (_ssl.c")[0])

print("sockets")
cc, sc = contexts()
a, b = socket.socketpair()
out = []
def serve():
    with sc.wrap_socket(b, server_side=True) as ss:
        out.append(("server", ss.version()))
        data = ss.recv(1024)
        ss.sendall(data.upper())
        out.append(("server got", data))
t = threading.Thread(target=serve)
t.start()
with cc.wrap_socket(a, server_hostname="localhost") as cs:
    print(cs.version(), cs.cipher()[1], cs.getpeercert()["subject"][-1])
    cs.sendall(b"ping over a socket")
    print(cs.recv(1024))
    print(cs.selected_alpn_protocol(), cs.compression(), cs.pending())
    pass
t.join()
for o in out:
    print(o)

print("timeout")
a, b = socket.socketpair()
a.settimeout(0.2)
with cc.wrap_socket(a, server_hostname="localhost", do_handshake_on_connect=False) as cs:
    try:
        cs.do_handshake()
    except TimeoutError as e:
        print("TimeoutError", e)
b.close()

print("nonblocking")
a, b = socket.socketpair()
a.setblocking(False)
cs = cc.wrap_socket(a, server_hostname="localhost", do_handshake_on_connect=False)
try:
    cs.do_handshake()
except ssl.SSLWantReadError as e:
    print("SSLWantReadError", e.errno)
print(len(b.recv(4096)) > 0)
b.close()
try:
    import select
    select.select([cs], [], [], 5)
    cs.do_handshake()
except ssl.SSLError as e:
    print(type(e).__name__, e.reason)
cs.close()

print("alpn")
cc, sc = contexts()
cc.set_alpn_protocols(["h2", "http/1.1"])
sc.set_alpn_protocols(["foo", "http/1.1"])
c, s, *bios = bio_pair(cc, sc)
pump(c, s, *bios)
print(c.selected_alpn_protocol(), s.selected_alpn_protocol())
sc.set_alpn_protocols(["foo"])
c, s, *bios = bio_pair(cc, sc)
pump(c, s, *bios)
print(c.selected_alpn_protocol(), s.selected_alpn_protocol())

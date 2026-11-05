# _ssl's callbacks (queue 217): the SNI callback's alert (an integer returned, an exception, a wrong type) as the
# client sees it, the message callback's records and messages in the clear (TLS 1.2 and 1.3), a message callback's
# exception raised by the operation, and MemoryBIO.write's refusal of a non-contiguous view (also a view of one).
# The messages under the keys are left out (Go's tls.Conn keeps its keys: the survey's "As built: 217").
import os, socket, ssl, sys, threading

HERE = os.path.dirname(os.path.abspath(__file__))
CA = os.path.join(HERE, "ssl", "ca.pem")
CERT = os.path.join(HERE, "ssl", "keycert.pem")

def contexts():
    c = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    c.load_verify_locations(CA)
    s = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    s.load_cert_chain(CERT)
    return c, s

def serve(s, b, res):
    try:
        with s.wrap_socket(b, server_side=True) as x:
            x.recv(10)
    except Exception as e:
        res.append((type(e).__name__, getattr(e, "reason", None)))

print("sni")
for name, cb in [("alert", lambda *a: ssl.ALERT_DESCRIPTION_ACCESS_DENIED), ("raising", lambda *a: 1 / 0),
                 ("wrong type", lambda *a: "x"), ("none", lambda *a: None)]:
    c, s = contexts()
    s.sni_callback = cb
    a, b = socket.socketpair()
    res, unr = [], []
    old = sys.unraisablehook
    sys.unraisablehook = lambda u: unr.append((u.exc_type.__name__, u.err_msg.split(" set SNI")[0], u.object))
    t = threading.Thread(target=serve, args=(s, b, res))
    t.start()
    try:
        with c.wrap_socket(a, server_hostname="localhost") as x:
            x.send(b"hi")
            print(name, "client ok")
    except ssl.SSLError as e:
        print(name, "client", type(e).__name__, e.reason)
    t.join()
    sys.unraisablehook = old
    print(name, "server", res, unr)

print("msg_callback")
CLEAR = [("write", 256, 22), ("write", 22, 1), ("read", 256, 22), ("read", 22, 2), ("read", 22, 11),
         ("read", 22, 12), ("read", 22, 14), ("write", 22, 16), ("write", 256, 20), ("write", 20, 257),
         ("read", 20, 257), ("write", 256, 23), ("read", 256, 23)]
for v in (ssl.TLSVersion.TLSv1_2, ssl.TLSVersion.TLSv1_3):
    c, s = contexts()
    c.maximum_version = v
    msg = []
    def cb(conn, d, ver, ct, mt, data):
        msg.append((d, int(ver), int(ct), int(mt)))
        assert isinstance(conn, ssl.SSLSocket) and isinstance(data, bytes)
        assert len(data) == 5 if ct == 256 else len(data) >= 1
    c._msg_callback = cb
    a, b = socket.socketpair()
    res = []
    t = threading.Thread(target=serve, args=(s, b, res))
    t.start()
    with c.wrap_socket(a, server_hostname="localhost") as x:
        x.send(b"hi")
    t.join()
    print(v.name, res)
    for d, ct, mt in CLEAR:
        if v == ssl.TLSVersion.TLSv1_3 and (d, ct, mt) == ("read", 22, 11):
            continue  # TLS 1.3's certificate is under the keys
        vers = sorted({m[1] for m in msg if m[0] == d and m[2] == ct and m[3] == mt})
        print(" ", d, ct, mt, vers)

    def bad(*a):
        raise KeyError("from the callback")
    c._msg_callback = bad
    a, b = socket.socketpair()
    t = threading.Thread(target=serve, args=(s, b, []))
    t.start()
    try:
        with c.wrap_socket(a, server_hostname="localhost"):
            print("no exception")
    except KeyError as e:
        print("KeyError", e)
    a.close()
    b.close()
    t.join()

print("bio")
m = memoryview(bytearray(b"noncontig"))[::-2]
for x in (m, memoryview(m)):
    try:
        ssl.MemoryBIO().write(x)
    except BufferError as e:
        print("BufferError", e)
mm = memoryview(m)
print(mm.obj is m.obj, mm.tolist(), mm.strides, mm.c_contiguous, bytes(mm))

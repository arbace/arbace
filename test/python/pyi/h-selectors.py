# PY6h: selectors over the select stand-in (select, poll, epoll) and _socket's stand-in: socket pairs, TCP on loopback
import selectors, socket, select, os

for cls in (selectors.SelectSelector, selectors.PollSelector, selectors.EpollSelector):
    a, b = socket.socketpair()
    with cls() as sel:
        sel.register(a, selectors.EVENT_READ, "a")
        sel.register(b, selectors.EVENT_WRITE, "b")
        ev = sel.select(0)
        print(cls.__name__, sorted((k.data, m) for k, m in ev))
        b.send(b"x")
        ev = sel.select(1)
        print(cls.__name__, sorted((k.data, m) for k, m in ev), a.recv(10))
        sel.unregister(b)
        print(len(sel.get_map()), sel.get_key(a).events)
    a.close(); b.close()

r, w = os.pipe()
print(select.select([r], [w], [], 0) == ([], [w], []))
os.write(w, b"y")
print(select.select([r], [], [], 0)[0] == [r])
os.close(r); os.close(w)

srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
srv.bind(("127.0.0.1", 0))
srv.listen()
port = srv.getsockname()[1]
c = socket.create_connection(("127.0.0.1", port))
s, addr = srv.accept()
c.sendall(b"hello")
print(s.recv(5), addr[0], s.family == socket.AF_INET, s.type == socket.SOCK_STREAM)
s.settimeout(0.05)
try:
    s.recv(1)
except TimeoutError as e:
    print("timeout", e)
print(s.gettimeout(), c.getblocking(), repr(socket.AddressFamily.AF_INET))
for x in (c, s, srv):
    x.close()
print(socket.getaddrinfo("127.0.0.1", 80, socket.AF_INET, socket.SOCK_STREAM)[0][4])
print(socket.inet_aton("10.1.2.3"), socket.inet_ntoa(b"\x7f\x00\x00\x01"), socket.htons(1), socket.ntohl(1))
print(socket.inet_ntop(socket.AF_INET6, socket.inet_pton(socket.AF_INET6, "fe80::1:2")))

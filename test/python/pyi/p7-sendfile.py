# PY7: os.sendfile releases the GIL while it blocks (a blocking socket's sendfile held it, so the thread reading the other
# end never ran: test_socket's SendfileUsingSendfileTest.testOffset hung)
import os, socket, tempfile, threading
data = bytes(range(256)) * (4 * 4096)  # 4 MiB, past the socket's buffer
with tempfile.TemporaryFile() as f:
    f.write(data)
    f.flush()
    a, b = socket.socketpair()
    got = []
    def reader():
        while True:
            chunk = b.recv(65536)
            if not chunk:
                break
            got.append(chunk)
    t = threading.Thread(target=reader)
    t.start()
    offset, total = 5000, 0
    while True:
        n = os.sendfile(a.fileno(), f.fileno(), offset, len(data))
        if n == 0:
            break
        offset += n
        total += n
    a.close()
    t.join()
    b.close()
    print(total == len(data) - 5000, b"".join(got) == data[5000:])

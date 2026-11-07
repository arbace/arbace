# PY6d: zlib (zlib 1.3.2's deflate and inflate ported to Go): every level, strategy, window and memLevel, gzip and raw,
# flushes, dictionaries, copies, decompressobj's max_length / unconsumed_tail / unused_data, _ZlibDecompressor, errors
import zlib, gzip, io, hashlib
text = b"".join(b"line %d: the quick brown fox jumps over the lazy dog %d\n" % (i, i * i % 97) for i in range(300))
noise = hashlib.sha256(b"seed").digest() * 40
data = text + noise + text[:2000]
for level in (0, 1, 6, 9):
    for wbits in (9, 15, -15, 31):
        for mem in (1, 9):
            for strat in range(5):
                c = zlib.compressobj(level, zlib.DEFLATED, wbits, mem, strat)
                out = c.compress(data[:7000]) + c.flush(zlib.Z_SYNC_FLUSH) + c.compress(data[7000:]) + c.flush()
                assert zlib.decompress(out, wbits) == data
                print(level, wbits, mem, strat, len(out), hashlib.md5(out).hexdigest()[:12])
print(zlib.compress(b"hello"), zlib.compress(b"hello", 9, -9), zlib.compress(b""), zlib.adler32(data), zlib.crc32(data), zlib.adler32(b"x", 2**33 + 5))
c = zlib.compressobj(zdict=b"quick brown fox"); z = c.compress(text[:500]) + c.flush()
d = zlib.decompressobj(zdict=b"quick brown fox"); print(len(z), d.decompress(z) == text[:500], d.eof)
cc = zlib.compressobj(); cc.compress(text[:100]); c2 = cc.copy(); print(cc.flush() == c2.flush())
comp = zlib.compress(data) + b"TRAILER"
d = zlib.decompressobj()
pieces, buf = [], comp
while buf:
    pieces.append(d.decompress(buf, 1000)); buf = d.unconsumed_tail
    if d.eof: break
print(len(pieces), sum(map(len, pieces)), d.eof, d.unused_data, d.unconsumed_tail, d.flush())
d = zlib.decompressobj(); print(len(d.decompress(comp[:100])), d.eof, len(d.copy().flush()))
z = zlib._ZlibDecompressor(); r = z.decompress(comp, 50); print(len(r), z.needs_input, z.eof, len(z.decompress(b"", 10**6)), z.eof, z.unused_data)
g = gzip.compress(data, mtime=0); print(len(g), g[:10], gzip.decompress(g) == data, zlib.decompress(g, 47) == data)
print(gzip.GzipFile(fileobj=io.BytesIO(g)).read(20))
for bad in (b"x\x9c\x00", b"garbage!", zlib.compress(b"abc")[:-1], zlib.compress(b"abc")[:-2] + b"\0\0"):
    try: zlib.decompress(bad)
    except zlib.error as e: print(e)
for bad in (lambda: zlib.compress(b"", 10), lambda: zlib.compressobj(wbits=-8), lambda: zlib.decompressobj(wbits=99), lambda: zlib.decompress(b"", bufsize=-1),
            lambda: zlib.decompressobj().decompress(b"", -1), lambda: zlib.decompressobj().flush(0), lambda: zlib.compress("s"), lambda: zlib.decompressobj(zdict=1)):
    try: bad()
    except Exception as e: print(type(e).__name__, e)
c = zlib.compressobj(); c.flush()
for bad in (lambda: c.compress(b"x"), lambda: c.copy()):
    try: bad()
    except Exception as e: print(type(e).__name__, e)
print(zlib.ZLIB_VERSION, zlib.MAX_WBITS, zlib.Z_RLE, zlib.Z_TREES, type(zlib.compressobj()).__name__, zlib.error.__module__)

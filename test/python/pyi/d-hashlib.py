# PY6d: hashlib over _md5, _sha1, _sha2, _sha3 (SHAKE) and _blake2 (every parameter), copies, attributes, messages
import hashlib, _md5, _sha3, _blake2
for n in sorted(hashlib.algorithms_guaranteed):
    h = hashlib.new(n, b"abc" * 100)
    h2 = h.copy(); h2.update(b"more")
    if n.startswith("shake"):
        print(n, h.hexdigest(20), h2.hexdigest(7), h.digest(3), h.name, h.digest_size, h.block_size)
    else:
        print(n, h.hexdigest(), h2.hexdigest(), h.name, h.digest_size, h.block_size)
print(hashlib.blake2b(b"x", digest_size=20, key=b"k"*64, salt=b"s"*16, person=b"p"*16, fanout=2, depth=3, leaf_size=5, node_offset=7, node_depth=1, inner_size=9, last_node=True).hexdigest())
print(hashlib.blake2s(b"y"*200, digest_size=11, key=b"q", salt=b"s"*8, person=b"p"*3, node_offset=2**47, last_node=True).hexdigest())
print(hashlib.blake2b(b"z"*128).hexdigest(), hashlib.blake2b(b"z"*129, key=b"").hexdigest())
print(_sha3.sha3_224()._capacity_bits, _sha3.shake_256()._suffix, _blake2.blake2s.MAX_KEY_SIZE)
for f in (lambda: _md5.md5("x"), lambda: _md5.md5(1), lambda: type(_md5.md5())(), lambda: _sha3.shake_128().digest(2**29), lambda: hashlib.blake2b(digest_size=65), lambda: hashlib.blake2b(salt=b"x"*17)):
    try: f()
    except Exception as e: print(type(e).__name__, e)
print(hashlib.file_digest(__import__('io').BytesIO(b"abc"), "sha256").hexdigest())

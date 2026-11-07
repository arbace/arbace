# PY6a: _io from _pyio (StringIO, BytesIO, open in text and binary modes, the ABCs), against CPython
import io, os
s = io.StringIO(); s.write('hello\n'); print(repr(s.getvalue()), isinstance(s, io.TextIOBase), isinstance(s, io.IOBase))
b = io.BytesIO(b'abc'); print(b.read(2), b.read(), b.tell(), isinstance(b, io.BufferedIOBase))
p = 'iofile-f-io.tmp'
with open(p, 'w') as f:
    f.write('line1\nline2\n')
with open(p) as f:
    print(f.readlines())
with open(p, 'rb') as f:
    print(f.read(), type(f).__name__)
os.remove(p)
print(io.SEEK_END, io.DEFAULT_BUFFER_SIZE, issubclass(io.UnsupportedOperation, ValueError))

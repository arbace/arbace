# PY6a: _struct, array's minimum, _typing, gc, sys.monitoring, float.hex, against CPython
import typing, gc, sys
from typing import ParamSpec
P = ParamSpec("P")
import struct, array
print(struct.pack('<hI', -2, 7), struct.unpack('>Q', b'\x00' * 7 + b'\x05'), struct.calcsize('@bi'), struct.calcsize('=bi'), struct.pack('3s?d', b'ab', True, 1.5))
s = struct.Struct('!H'); print(s.size, s.pack(513), s.unpack(b'\x02\x01'), list(struct.iter_unpack('<b', b'\x01\xff')))
try:
    struct.pack('b', 200)
except struct.error as e:
    print(e)
a = array.array('i', [1, 2, 3]); a.append(4); print(a, len(a), a[1], a.tobytes()[:4], array.array('d', [1.5]).itemsize, a == array.array('i', [1, 2, 3, 4]))
print(P.args if hasattr(P,'args') else 'na', typing.Union, gc.collect(), sys.monitoring.events.LINE, float.fromhex('0x1.8p1'), (3.0).hex(), (0.1).hex(), float.fromhex('-0x1p-1074'), (5e-324).hex())

# a float NaN's bits are CPython's Py_NAN (PY6g's REQUEST for PY6-core)
import struct
for s in ('nan', '-nan', 'NaN', '+nan'):
    print(s, struct.pack('<d', float(s)).hex())
print(struct.pack('<d', float.fromhex('nan')).hex(), struct.pack('<d', eval('float("nan")')).hex())

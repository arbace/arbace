# PY6a: _codecs' registry and utf-8/ascii/latin-1 through codecs and encodings, against CPython
import codecs
print(codecs.lookup('UTF-8').name, codecs.lookup('latin1').name, codecs.encode('é', 'utf-8'), codecs.decode(b'\xc3\xa9', 'utf-8'))
d = codecs.getincrementaldecoder('utf-8')()
print(repr(d.decode(b'\xc3')), repr(d.decode(b'\xa9')), codecs.utf_8_decode(b'ab\xc3', 'strict', False))
try:
    codecs.lookup('nope')
except LookupError as e:
    print(e)
print(codecs.lookup_error('strict').__name__, 'é'.encode('ascii', 'replace'))

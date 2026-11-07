# PY6a: int.to_bytes and int.from_bytes, against CPython
print((1024).to_bytes(4, 'little'), (-1).to_bytes(2, 'big', signed=True), int.from_bytes(b'\x01\x02', 'big'), int.from_bytes(b'\xff', 'little', signed=True), bool.from_bytes(b'\x01'), (0).to_bytes())
try:
    (256).to_bytes(1, 'big')
except OverflowError as e:
    print(e)

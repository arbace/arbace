# PY5b0's smoke programs (bin/pyi runtime check): run by pyi and by the oracle, compared.
print(1 + 2)
print(7 // 2, -7 // 2, 7 % -3, 2 * 3 - 4, -5, ~5)
print(3 < 4, 3 == 4, 1 if 0 else 2, not 0)
x = 10
x += 5
print(x, "a" + "b", repr("it's"), [1, (2,), (3, 4)], {"k": 1})

# Sequence repetition past PY_SSIZE_T_MAX or past what can be allocated raises without allocating (202b; test_list's
# test_overflow made pyi allocate until the OOM killer).
import sys
n4 = (sys.maxsize * 2 + 2) // 4
half = sys.maxsize // 2 + 1
cases = [
    ("list * n", lambda: [4, 5, 6, 7] * n4),
    ("n * list", lambda: n4 * [4, 5, 6, 7]),
    ("tuple * n", lambda: (1, 2) * half),
    ("list * huge", lambda: [1] * half),
    ("str * n", lambda: "ab" * half),
    ("bytes * n", lambda: b"ab" * half),
    ("bytearray * n", lambda: bytearray(b"ab") * half),
]
for name, f in cases:
    try:
        f()
        print(name, "no error")
    except (MemoryError, OverflowError) as e:
        print(name, type(e).__name__, repr(str(e)))
l = [4, 5, 6, 7]
try:
    l *= n4
except MemoryError as e:
    print("list *=", type(e).__name__, l)
print([1, 2] * 3, (0,) * 2, "ab" * 2, b"x" * 3, [] * n4, "" * half)

# PY6a: sequence patterns as CPython's codegen_pattern_sequence compiles them: a star wildcard indexes the subject
# (never unpacked, test_patma's test_patma_192 on range(1 << 30)), only wildcards pop it, a lone star checks no length
class Seq(list):  # a list: the sequence flag (collections.abc's registrations aside)
    def __init__(self, n):
        super().__init__([None] * n)
        self.n = n

    def __len__(self):
        print("len", end=" ")
        return self.n

    def __getitem__(self, i):
        print("get", i, end=" ")
        if not 0 <= i < self.n:
            raise IndexError(i)
        return i * 10

    def __iter__(self):
        print("iter", end=" ")
        return iter([i * 10 for i in range(self.n)])



def m(s):
    match s:
        case [a, b, *_, c]:
            return ("abc", a, b, c)
        case [a, *_]:
            return ("a", a)
        case [*_, z] if z == 0:
            return ("z", z)
        case [_, _, *_]:
            return "wild2"
        case [*_]:
            return "wild"


for n in (0, 1, 2, 3, 5):
    print(n, m(Seq(n)))
for v in ([], [1], [1, 2], (1, 2, 3, 4), range(1 << 30), "ab"):
    print(m(v))
match range(1 << 30):
    case x, y, *_:
        print(x, y)
match [1, 2, 3]:
    case [*rest]:
        print(rest)
match [1, 2, 3]:
    case [p, *rest, q]:
        print(p, rest, q)

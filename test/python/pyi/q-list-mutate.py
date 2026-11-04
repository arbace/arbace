# A list changed by the comparisons or __index__ of its own operations (202e's report: test_xml_etree's
# BadElementTest): list.remove and slice assignment read the length again, as CPython does.
class E:
    def __init__(self, l): self.l = l
    def __eq__(self, o):
        self.l.clear(); return False
l = []
l.extend([E(l), E(l), 1])
try:
    l.remove(1); print("removed", len(l))
except ValueError as e:
    print("ValueError", e)
class I:
    def __init__(self, l, n=10): self.l, self.n = l, n
    def __index__(self):
        self.l.clear(); return self.n
m = list(range(5))
m[I(m):] = [1, 2]
print(m)
m = list(range(5))
m[:I(m, 3)] = [7]
print(m)
m = list(range(8))
del m[I(m, 2):6]
print(m)
m = list(range(8))
print(m[I(m, 1):4])
def gen(l):
    l.clear()
    yield 9
m = list(range(6))
m[2:4] = gen(m)
print(m)
class F:
    def __init__(self, l): self.l = l
    def __eq__(self, o):
        del self.l[:]; return True
k = [F(None)]
k[0].l = k
k.remove(5)
print(k)

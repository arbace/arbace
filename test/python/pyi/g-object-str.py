# object.__str__ is object_str: the repr, never the type's own __str__ (PY6g's REQUEST for PY6-core)
class A:
    def __repr__(self): return 'A-repr'
    __str__ = object.__str__
class B(A):
    def __str__(self): return 'B-str'
print(str(A()), str(B()), object.__str__(B()), object.__str__(1), object.__str__('x'), str(object()) [:8])
class C(int):
    __str__ = object.__str__
print(str(C(5)))

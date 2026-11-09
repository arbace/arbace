# An independent model of ops/matmul.py and aug-matmul.py: none of the operands' types (int, float, bool, complex, str, bytes, list, tuple,
# set, dict, NoneType) implements @ or @= (Reference, Expressions: Binary arithmetic operations: "No builtin Python types implement this
# operator"), so each of the 15 x 15 cells raises TypeError
BEGIN { for (i = 0; i < 15; i++) for (j = 0; j < 15; j++) print "(" i ", " j ") -> TypeError" }

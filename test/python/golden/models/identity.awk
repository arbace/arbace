# An independent model of ops/cmp-is.py and cmp-is-not.py (-v neg=0|1): `a is b` is true exactly when a and b are the same object (Reference,
# Expressions: Identity comparisons); the 15 operands are 15 distinct objects of distinct values (no two literals of equal value), so the cell
# (i, j) is true iff i == j; `is not` yields the inverse truth value
BEGIN { for (i = 0; i < 15; i++) for (j = 0; j < 15; j++) print "(" i ", " j ") -> " (((i == j) != neg) ? "True" : "False") }

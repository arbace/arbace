# An independent model of ops/cmp-lt.py, cmp-le.py, cmp-gt.py and cmp-ge.py (-v op=lt|le|gt|ge) over the 15 operands (Reference, Expressions:
# Value comparisons): real numbers (7, -3, 0, 2.5, -0.5, True = 1, 10**20, all exact doubles) compare by their mathematical value, bool
# included; a complex number supports no order, so any cell with the complex operand is a TypeError; str, bytes, list, tuple and set order
# among themselves only (lexicographically, a set by inclusion), and each of the 15 operands is the only one of its kind, so those cells are
# the comparison of a value with itself (lt and gt False, le and ge True); dict, None, and any two operands of different kinds (apart from the
# real numbers) raise TypeError
BEGIN {
  split("r r r r r r c s b l t e d n r", kind, " ")
  split("7 -3 0 2.5 -0.5 1 0 0 0 0 0 0 0 0 1e20", val, " ")
  for (i = 0; i < 15; i++) for (j = 0; j < 15; j++) {
    ki = kind[i + 1]; kj = kind[j + 1]
    if (ki == "r" && kj == "r") {
      x = val[i + 1] + 0; y = val[j + 1] + 0
      r = (op == "lt") ? (x < y) : (op == "le") ? (x <= y) : (op == "gt") ? (x > y) : (x >= y)
      out = r ? "True" : "False"
    } else if (i == j && ki ~ /^[sblte]$/) {
      out = (op == "le" || op == "ge") ? "True" : "False"
    } else out = "TypeError"
    print "(" i ", " j ") -> " out
  }
}

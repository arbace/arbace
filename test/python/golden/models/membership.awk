# An independent model of ops/cmp-in.py and cmp-not-in.py (-v neg=0|1) over the 15 operands (Reference, Expressions: Membership test operations;
# library, Built-in Types: Sequence, Set and Mapping Types). The cell (i, j) is `a in b`, a operand i, b operand j:
#  b an int, float, bool, complex or None (operands 0-6, 13, 14): not a container, TypeError
#  b the str 'ab': True when a is a substring, which requires a str: only 'ab' in 'ab'; any other a raises TypeError
#  b the bytes b'xy': a bytes is a subsequence test (only b'xy' in b'xy' is True); an int must be in range(256), else ValueError (-3, 10**20);
#     an in-range int (7, 0, True) is a byte value, 120 and 121 are not those: False; a float, complex, str, list, tuple, set, dict or None: TypeError
#  b the list [1, 2] and the tuple (3,): any element equal to a (==): True == 1 is the one equal pair, so True in [1, 2]; all other cells False
#  b the set {1, 2}: a must be hashable, the dict and the list are not (TypeError); a set is searched as an equal frozenset (False here);
#     the others by hash and ==: True == 1, so True in {1, 2}; the rest False
#  b the dict {'k': 1}: the keys are searched, a must be hashable: list, set and dict raise TypeError; the rest are not 'k': False
# `not in` is the inverse truth value (the exceptions are the same)
BEGIN {
  split("i i i f f i c s b l t e d n i", kind, " ")
  split("7 -3 0 2.5 -0.5 1 0 0 0 0 0 0 0 0 1e20", val, " ")
  for (i = 0; i < 15; i++) for (j = 0; j < 15; j++) {
    ka = kind[i + 1]; kb = kind[j + 1]; r = ""
    if (kb ~ /^[ifcn]$/) r = "TypeError"
    else if (kb == "s") r = (ka == "s") ? "T" : "TypeError"
    else if (kb == "b") {
      if (ka == "b") r = "T"
      else if (ka == "i") r = (val[i + 1] + 0 >= 0 && val[i + 1] + 0 <= 255) ? "F" : "ValueError"
      else r = "TypeError"
    }
    else if (kb == "l") r = (i == 5) ? "T" : "F"
    else if (kb == "t") r = "F"
    else if (kb == "e") r = (ka == "l" || ka == "d") ? "TypeError" : (i == 5) ? "T" : "F"
    else if (kb == "d") r = (ka == "l" || ka == "e" || ka == "d") ? "TypeError" : "F"
    if (r == "T") r = neg ? "False" : "True"; else if (r == "F") r = neg ? "True" : "False"
    print "(" i ", " j ") -> " r
  }
}

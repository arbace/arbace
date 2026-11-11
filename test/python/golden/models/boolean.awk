# an independent model of ops/boolean.py's rows: `a and b` yields a if a is false else b; `a or b` yields a if a is true else b;
# the operands evaluated are a, then b only when the first did not decide; truth values by the Reference (Boolean operations / Truth Value Testing)
BEGIN {
  split("0|1|''|'a'|None|[]|[0]", rep, "|"); split("0 1 0 1 0 0 1", tr, " ")
  for (i = 1; i <= 7; i++) for (j = 1; j <= 7; j++) {
    a = rep[i]; b = rep[j]
    if (!tr[i]) { r1 = a; c1 = "[" a "]" } else { r1 = b; c1 = "[" a ", " b "]" }
    if (tr[i]) { r2 = a; c2 = "[" a "]" } else { r2 = b; c2 = "[" a ", " b "]" }
    print a " " b " " r1 " " c1 " " r2 " " c2
  }
  print "True False True True"
}

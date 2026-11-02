struct S;
int x;
int m1 = _Generic (x, struct S: 1, default: 2);
# 4 "i/lm.h" 3
int m2 = _Generic (x, struct S: 1, default: 2);
#include <lmc.h>

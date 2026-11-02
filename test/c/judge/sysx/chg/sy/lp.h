struct S;
int x;
int l1 = _Generic (x, struct S: 1, default: 2);
# 4 "sy/lp.h"
int l2 = _Generic (x, struct S: 1, default: 2);
#include <lpc.h>

// 7.20: <stdckdint.h>'s checked arithmetic.
#include <stdckdint.h>
#include <stdbool.h>
bool add_overflows(int a, int b, int *r) { return ckd_add(r, a, b); }

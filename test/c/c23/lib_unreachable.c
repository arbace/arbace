// 7.21.1: unreachable() from <stddef.h> (gcc's freestanding header first, CS32 (a)).
#include <stddef.h>
int pick(int x) { if (x > 0) return 1; unreachable(); }

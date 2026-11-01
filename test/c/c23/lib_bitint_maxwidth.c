// 5.2.4.2.1: BITINT_MAXWIDTH in <limits.h>.
#include <limits.h>
static_assert(BITINT_MAXWIDTH >= 64);
unsigned _BitInt(BITINT_MAXWIDTH > 128 ? 128 : 64) wide;

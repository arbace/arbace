/* ci-run verdict: exit 0. A NaN converted to unsigned long and unsigned int as gcc -O0 does on x86-64 (doc/C23-SURVEY.md, "As built: review
   006's fixes", finding 10): comisd 2^63 is unordered, so cvttsd2si gives 0x8000000000000000; huge, negative and large
   values beside it. Checks itself: each value against the x86-64 instruction sequence's (Intel's SDM: cvttsd2si of a NaN
   or of a value out of range is the integer indefinite 0x8000000000000000; a value at or above 2^63 converted as
   cvttsd2si (d - 2^63) xor 2^63; to unsigned int through the 64-bit conversion, truncated) and C's for 9.3e18 (in range,
   exact: 0x81103cb9fb220000), worked out by hand; a difference returns 1. */
#include <stdio.h>
#include <math.h>
int main(void) {
  volatile double d = NAN; volatile float f = NAN; volatile double big = 1e30, neg = -1.0, h = 9.3e18;
  unsigned long a[5] = { (unsigned long)d, (unsigned long)f, (unsigned long)big, (unsigned long)neg, (unsigned long)h };
  unsigned u[2] = { (unsigned)d, (unsigned)f }; long l = (long)d;
  printf("%lx %lx %lx %lx %lx\n", a[0], a[1], a[2], a[3], a[4]);
  printf("%x %x %lx\n", u[0], u[1], l);
  return a[0] == 0x8000000000000000ul && a[1] == 0x8000000000000000ul && a[2] == 0 && a[3] == 0xfffffffffffffffful
         && a[4] == 0x81103cb9fb220000ul && u[0] == 0 && u[1] == 0 && l == (long)0x8000000000000000ul ? 0 : 1;
}

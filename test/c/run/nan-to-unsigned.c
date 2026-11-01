/* A NaN converted to unsigned long and unsigned int as gcc -O0 does on x86-64 (doc/C23-SURVEY.md, "As built: review
   006's fixes", finding 10): comisd 2^63 is unordered, so cvttsd2si gives 0x8000000000000000; huge, negative and large
   values beside it. */
#include <stdio.h>
#include <math.h>
int main(void) {
  volatile double d = NAN; volatile float f = NAN; volatile double big = 1e30, neg = -1.0, h = 9.3e18;
  printf("%lx %lx %lx %lx %lx\n", (unsigned long)d, (unsigned long)f, (unsigned long)big, (unsigned long)neg, (unsigned long)h);
  printf("%x %x %lx\n", (unsigned)d, (unsigned)f, (long)d);
  return 0;
}

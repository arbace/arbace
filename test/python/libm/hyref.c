/* hyref: reads "x y port" bit patterns, compares the port with musl's hypot (the VM's libc is musl) */
#include <math.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
int main(void) {
  unsigned long long a, b, p; long n = 0, bad = 0;
  while (scanf("%llx %llx %llx", &a, &b, &p) == 3) {
    double x, y, r; memcpy(&x, &a, 8); memcpy(&y, &b, 8);
    r = hypot(x, y); unsigned long long rb; memcpy(&rb, &r, 8);
    n++;
    if (rb != p && !(isnan(r) && (p & 0x7ff0000000000000ULL) == 0x7ff0000000000000ULL && (p & 0xfffffffffffffULL))) {
      if (bad++ < 5) printf("MISMATCH %016llx %016llx musl %016llx port %016llx\n", a, b, rb, p);
    }
  }
  printf("hypot: %ld calls, %ld mismatches\n", n, bad);
  return bad != 0;
}

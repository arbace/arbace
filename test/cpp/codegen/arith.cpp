// integer and floating arithmetic, the usual arithmetic conversions, wrapping unsigned arithmetic
#include <cstdio>
int main() {
  int a = 17, b = -5;
  unsigned u = 3000000000u;
  long l = 1L << 40;
  short s = 30000;
  char c = 'A';
  printf("%d %d %d %d\n", a + b, a * b, a / b, a % b);
  printf("%u %u\n", u + u, u / 7);
  printf("%ld %ld\n", l, l >> 3);
  printf("%d %d\n", s + s, c + 1);
  unsigned char uc = 250; uc += 10;
  printf("%d\n", uc);
  double d = 7.0 / 2; float f = 1.25f;
  printf("%.3f %.3f %.3f\n", d, f * 3, d - f);
  printf("%d %d\n", (int)d, (int)-2.75);
  printf("%d %d %d\n", a & 6, a | 64, a ^ 255);
  printf("%d %d\n", ~a, -a);
  printf("%d %d %d\n", a < b, u > 1u, -1 < 0u);
  long long big = 9000000000LL * 3;
  printf("%lld %llu\n", big, (unsigned long long)big * 3);
  int x = 5; x <<= 2; x -= 3; x *= 2; x /= 3; x %= 5;
  printf("%d\n", x);
  double acc = 0; for (int i = 1; i <= 10; ++i) acc += 1.0 / i;
  printf("%.6f\n", acc);
  return 0;
}

// bit manipulation on unsigned and signed integers of every width
#include <cstdio>
int popcount(unsigned long v) { int c = 0; while (v) { c += v & 1; v >>= 1; } return c; }
unsigned rotl(unsigned v, int k) { return (v << k) | (v >> (32 - k)); }
bool is_pow2(unsigned long long v) { return v && !(v & (v - 1)); }
int main() {
  unsigned x = 0xF0F0u;
  printf("%d %u %x\n", popcount(x), rotl(x, 8), x >> 4);
  printf("%d %d %d\n", is_pow2(1024), is_pow2(1000), is_pow2(1ULL << 63));
  signed char sc = -128; unsigned char uc = 0xff;
  printf("%d %d %d\n", sc >> 1, uc >> 1, (signed char)uc);
  short sh = -2; unsigned short us = (unsigned short)sh;
  printf("%d %u %d\n", sh, us, us >> 15);
  long neg = -1; unsigned long un = (unsigned long)neg;
  printf("%ld %lu %d\n", neg >> 63, un >> 63, (int)(un >> 32));
  int m = 0; for (int i = 0; i < 8; i += 2) m |= 1 << i;
  printf("%d %d %d\n", m, m ^ 0xff, m & ~5);
  return 0;
}

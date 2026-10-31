// Integers of odd widths (review 007's finding 4): bit-field access units clang loads and stores as i24, i40, i48 and i56
// (the next field is not overlapped), so the lowering reads and writes their store size only, masked to the width.
#include <cstdio>
struct S24 { unsigned a : 24; unsigned char c; };
struct S40 { unsigned long a : 40; unsigned char b; unsigned char c; };
struct S48 { long a : 48; unsigned short n; };
struct S56 { unsigned long a : 56; unsigned char z; };
struct P17 { unsigned a : 17; unsigned b : 15; };
S24 s24 = {1, 2};
S40 s40 = {3, 7, 9};
S48 s48 = {-5, 0xbeef};
S56 s56 = {0x11223344556677UL, 0x88};
P17 p17 = {0x1ffff, 0x5555};
int main() {
  s24.a = 5; s24.a += 0xfffffe;
  std::printf("%u %u\n", s24.a, s24.c);
  s40.a = 0xffffffffffUL; s40.a += 2;
  std::printf("%lu %u %u\n", (unsigned long)s40.a, s40.b, s40.c);
  s48.a -= 1;
  std::printf("%ld %x\n", (long)s48.a, s48.n);
  s56.a ^= 0xffffffffffffffUL;
  std::printf("%lx %x\n", (unsigned long)s56.a, s56.z);
  p17.a += 1; p17.b <<= 1;
  std::printf("%x %x\n", p17.a, p17.b);
}

// synth-c23/decl-bitint-wide.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// wide bit-precise integers (128, 200 and 256 bits): arithmetic, shifts, division
// ISO/IEC 9899:2024 6.2.5, 6.7.3; Annex A: type-specifier (_BitInt)
// M.2: bit-precise integer types _BitInt(N) wider than long long
#include <stdio.h>

typedef unsigned _BitInt(256) u256;
static unsigned long long part(u256 v, int i) { return (unsigned long long)(v >> (64 * i)); }

int main(void) {
  u256 a = 0xFFFFFFFFFFFFFFFFuwb;
  u256 p = a * a;
  unsigned _BitInt(128) q = (unsigned _BitInt(128))1 << 100;
  _BitInt(200) n = -((_BitInt(200))1 << 150);
  u256 d = p / 0x10001uwb;
  u256 m = p % 1000000007uwb;
  printf("%s = %llx\n", "part(p, 0)", part(p, 0));
  printf("%s = %llx\n", "part(p, 1)", part(p, 1));
  printf("%s = %llx\n", "(unsigned long long)(q >> 64)", (unsigned long long)(q >> 64));
  printf("%s = %d\n", "n < 0", n < 0);
  printf("%s = %llx\n", "(unsigned long long)(-n >> 140)", (unsigned long long)(-n >> 140));
  printf("%s = %llx\n", "part(d, 0)", part(d, 0));
  printf("%s = %llx\n", "part(d, 1)", part(d, 1));
  printf("%s = %llu\n", "part(m, 0)", part(m, 0));
  printf("%s = %d\n", "(int)(p == a * a)", (int)(p == a * a));
  return 0;
}

// synth-c23/decl-bitint.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// bit-precise integer types: widths, signedness, wb and uwb suffixes, wrap-around
// ISO/IEC 9899:2024 6.2.5, 6.4.4.1, 6.7.3; Annex A: type-specifier (_BitInt), integer-suffix (wb, uwb)
// M.2: bit-precise integer types _BitInt(N) and the wb suffix
#include <stdio.h>

int main(void) {
  _BitInt(7) s7 = 63;
  unsigned _BitInt(7) u7 = 127;
  u7 = u7 + 1;
  unsigned _BitInt(1) bit = 1;
  _BitInt(37) w = (_BitInt(37))1 << 35;
  unsigned _BitInt(64) big = 0xFFFFFFFFFFFFFFFFuwb;
  _BitInt(12) neg = -2048;
  signed _BitInt(2) two = -2;
  printf("%s = %d\n", "(int)s7", (int)s7);
  printf("%s = %d\n", "(int)u7", (int)u7);
  printf("%s = %d\n", "(int)bit", (int)bit);
  printf("%s = %lld\n", "(long long)w", (long long)w);
  printf("%s = %llu\n", "(unsigned long long)big", (unsigned long long)big);
  printf("%s = %d\n", "(int)neg", (int)neg);
  printf("%s = %d\n", "(int)two", (int)two);
  printf("%s = %d\n", "(int)(s7 + 1wb)", (int)(s7 + 1wb));
  printf("%s = %d\n", "_Generic(1wb, _BitInt(2): 1, default: 0)", _Generic(1wb, _BitInt(2): 1, default: 0));
  printf("%s = %d\n", "_Generic(1uwb, unsigned _BitInt(1): 1, default: 0)", _Generic(1uwb, unsigned _BitInt(1): 1, default: 0));
  printf("%s = %d\n", "_Generic(u7 + u7, unsigned _BitInt(7): 1, default: 0)", _Generic(u7 + u7, unsigned _BitInt(7): 1, default: 0));
  printf("%s = %d\n", "(int)(unsigned _BitInt(3))13", (int)(unsigned _BitInt(3))13);
  return 0;
}

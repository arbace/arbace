/* ci-run verdict: exit 0. gcc's __builtin_add_overflow_p, sub_overflow_p and mul_overflow_p (gcc 7): the exact result of the operands, in
   infinite precision, against the third argument's type (its value ignored, its side effects kept), narrow and wide,
   signed and unsigned operands mixed; folded in constant expressions; and __builtin_*_overflow over the wide types.
   Queue 126 (gcc.dg/torture/bitint-39.c, -45.c, -47.c, -49.c, -59.c, -60.c, -61.c, -67.c, -71.c, -73.c). Checks itself: each triple, the folded constants and the storing forms' results against
   the rule (the operands' exact result inside the third argument's type's range or not), worked out by hand; a
   difference returns 1. */
#include <stdio.h>
#include <limits.h>
static int fails;
/* the three results printed, and held to the triple the infinite-precision rule gives (worked out by hand) */
static void t3(int a, int s, int m, const char *want) {
  char b[4] = { (char)('0' + a), (char)('0' + s), (char)('0' + m), 0 };
  printf("%s ", b);
  for (int i = 0; i < 3; i++) if (b[i] != want[i]) { fprintf(stderr, "FAIL triple %s, want %s\n", b, want); fails++; break; } }
#define T3(a, b, c, w) t3(__builtin_add_overflow_p(a, b, c), __builtin_sub_overflow_p(a, b, c), __builtin_mul_overflow_p(a, b, c), w)
int main(void) {
  int side = 0;
  T3(INT_MAX, 1, 0, "100"); T3(INT_MIN, 1, 0, "010"); T3(INT_MIN, -1, 0, "101"); T3(-1, -1, 0u, "100"); T3(0, 0, 0u, "000"); T3(5, 7, (unsigned char)0, "010");
  T3(200u, 55, (unsigned char)0, "001"); T3(200u, 56, (unsigned char)0, "101"); T3(-128, 0, (signed char)0, "000"); T3(-129, 0, (signed char)0, "110");
  puts("");
  T3(ULLONG_MAX, 1ull, 0ull, "100"); T3(ULLONG_MAX, ULLONG_MAX, (unsigned __int128)0, "000"); T3(LLONG_MIN, -1ll, 0ll, "101"); T3(LLONG_MIN, -1ll, (__int128)0, "000");
  T3(0ull, 1, 0ll, "000"); T3(ULLONG_MAX, 0, 0ll, "110"); T3((unsigned)INT_MAX + 1, 0, 0, "110"); T3(3, 0, (_Bool)0 + 0, "000"); { int z_ = __builtin_add_overflow_p(1, 1, side++); printf("%d ", z_); if (z_) fails++; }
  puts("");
  /* wide: _BitInt and __int128 operands and results */
  _BitInt(256) a = (_BitInt(256))__INT_MAX__ + 1, b = __INT_MAX__;
  _BitInt(512) c = 0x7fffffffffffffffffffffffffffffffffffffffwb + (_BitInt(512))1, d = 0x7fffffffffffffffffffffffffffffffffffffffwb;
  T3(a, 0, 0, "110"); T3(b, -1, 0, "010"); T3(b, 0, 0, "000"); T3(c, 0, (_BitInt(160))0, "110"); T3(d, 0, (_BitInt(160))0, "000"); T3(d, -1, (_BitInt(160))0, "010");
  T3(d, d, (_BitInt(321))0, "000"); T3(d, d, (_BitInt(320))0, "000"); T3(d, d, (unsigned _BitInt(320))0, "000"); T3(-d, d, (unsigned _BitInt(400))0, "011");
  puts("");
  unsigned _BitInt(162) u = -1wbu; signed _BitInt(38) s = -1wb;
  T3(s, (signed char)0, u, "110"); T3(u, s, (_BitInt(163))0, "000"); T3(u, s, (unsigned _BitInt(162))0, "001"); T3(u, u, (_BitInt(324))0, "000"); T3(u, u, (_BitInt(325))0, "000");
  T3((unsigned __int128)-1, 1, (unsigned __int128)0, "100"); T3((__int128)-1, (__int128)-1, (_BitInt(65))0, "000"); T3(s, 1, (unsigned _BitInt(7))0, "011");
  printf("side %d\n", side);
  /* constant folding */
  enum { E1 = __builtin_add_overflow_p(INT_MAX, 1, 0), E2 = __builtin_mul_overflow_p(1 << 15, 1 << 15, 0u),
         E3 = __builtin_sub_overflow_p(0u, 1u, (long long)0), E4 = __builtin_mul_overflow_p(-1wb, 1wb, (unsigned _BitInt(200))0) };
  static int st = __builtin_add_overflow_p((_BitInt(300))1 << 298, (_BitInt(300))1 << 298, (_BitInt(300))0);
  printf("%d %d %d %d %d\n", E1, E2, E3, E4, st);
  /* the storing forms on wide types */
  _BitInt(200) r; unsigned __int128 q; int o;
  o = __builtin_add_overflow(c, c, &r); printf("%d %llx ", o, (unsigned long long)(r >> 128));
  if (side != 1 || !(E1 == 1 && E2 == 0 && E3 == 0 && E4 == 1 && st == 1)) fails++;
  if (o != 0 || r != (_BitInt(200))1 << 160) fails++;
  o = __builtin_mul_overflow((unsigned __int128)-1, 2, &q); printf("%d %llx %llx ", o, (unsigned long long)(q >> 64), (unsigned long long)q);
  if (o != 1 || q != (unsigned __int128)-2) fails++;
  /* (_BitInt(199))1 << 198 is -2^198 (gcc defines a signed shift into the sign bit as two's complement): 0 minus it fits */
  o = __builtin_sub_overflow(0, (_BitInt(199))1 << 198, &r); printf("%d %d\n", o, r < 0);
  if (o != 0 || r != (_BitInt(200))1 << 198) fails++;
  return fails ? 1 : 0;
}

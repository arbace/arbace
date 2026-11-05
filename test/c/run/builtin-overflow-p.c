/* gcc's __builtin_add_overflow_p, sub_overflow_p and mul_overflow_p (gcc 7): the exact result of the operands, in
   infinite precision, against the third argument's type (its value ignored, its side effects kept), narrow and wide,
   signed and unsigned operands mixed; folded in constant expressions; and __builtin_*_overflow over the wide types.
   Queue 126 (gcc.dg/torture/bitint-39.c, -45.c, -47.c, -49.c, -59.c, -60.c, -61.c, -67.c, -71.c, -73.c). */
#include <stdio.h>
#include <limits.h>
#define T3(a, b, c) printf("%d%d%d ", __builtin_add_overflow_p(a, b, c), __builtin_sub_overflow_p(a, b, c), __builtin_mul_overflow_p(a, b, c))
int main(void) {
  int side = 0;
  T3(INT_MAX, 1, 0); T3(INT_MIN, 1, 0); T3(INT_MIN, -1, 0); T3(-1, -1, 0u); T3(0, 0, 0u); T3(5, 7, (unsigned char)0);
  T3(200u, 55, (unsigned char)0); T3(200u, 56, (unsigned char)0); T3(-128, 0, (signed char)0); T3(-129, 0, (signed char)0);
  puts("");
  T3(ULLONG_MAX, 1ull, 0ull); T3(ULLONG_MAX, ULLONG_MAX, (unsigned __int128)0); T3(LLONG_MIN, -1ll, 0ll); T3(LLONG_MIN, -1ll, (__int128)0);
  T3(0ull, 1, 0ll); T3(ULLONG_MAX, 0, 0ll); T3((unsigned)INT_MAX + 1, 0, 0); T3(3, 0, (_Bool)0 + 0); printf("%d ", __builtin_add_overflow_p(1, 1, side++));
  puts("");
  /* wide: _BitInt and __int128 operands and results */
  _BitInt(256) a = (_BitInt(256))__INT_MAX__ + 1, b = __INT_MAX__;
  _BitInt(512) c = 0x7fffffffffffffffffffffffffffffffffffffffwb + (_BitInt(512))1, d = 0x7fffffffffffffffffffffffffffffffffffffffwb;
  T3(a, 0, 0); T3(b, -1, 0); T3(b, 0, 0); T3(c, 0, (_BitInt(160))0); T3(d, 0, (_BitInt(160))0); T3(d, -1, (_BitInt(160))0);
  T3(d, d, (_BitInt(321))0); T3(d, d, (_BitInt(320))0); T3(d, d, (unsigned _BitInt(320))0); T3(-d, d, (unsigned _BitInt(400))0);
  puts("");
  unsigned _BitInt(162) u = -1wbu; signed _BitInt(38) s = -1wb;
  T3(s, (signed char)0, u); T3(u, s, (_BitInt(163))0); T3(u, s, (unsigned _BitInt(162))0); T3(u, u, (_BitInt(324))0); T3(u, u, (_BitInt(325))0);
  T3((unsigned __int128)-1, 1, (unsigned __int128)0); T3((__int128)-1, (__int128)-1, (_BitInt(65))0); T3(s, 1, (unsigned _BitInt(7))0);
  printf("side %d\n", side);
  /* constant folding */
  enum { E1 = __builtin_add_overflow_p(INT_MAX, 1, 0), E2 = __builtin_mul_overflow_p(1 << 15, 1 << 15, 0u),
         E3 = __builtin_sub_overflow_p(0u, 1u, (long long)0), E4 = __builtin_mul_overflow_p(-1wb, 1wb, (unsigned _BitInt(200))0) };
  static int st = __builtin_add_overflow_p((_BitInt(300))1 << 298, (_BitInt(300))1 << 298, (_BitInt(300))0);
  printf("%d %d %d %d %d\n", E1, E2, E3, E4, st);
  /* the storing forms on wide types */
  _BitInt(200) r; unsigned __int128 q; int o;
  o = __builtin_add_overflow(c, c, &r); printf("%d %llx ", o, (unsigned long long)(r >> 128));
  o = __builtin_mul_overflow((unsigned __int128)-1, 2, &q); printf("%d %llx %llx ", o, (unsigned long long)(q >> 64), (unsigned long long)q);
  o = __builtin_sub_overflow(0, (_BitInt(199))1 << 198, &r); printf("%d %d\n", o, r < 0);
  return 0;
}

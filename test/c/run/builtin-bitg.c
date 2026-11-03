/* gcc's type-generic bit builtins (__builtin_popcountg, clzg, ctzg, ffsg, parityg, clrsbg; gcc 14), over every
   integer width ci has: the standard types, unsigned __int128 and _BitInt narrow and wide; clzg and ctzg with their
   second argument (the value for 0). Queue 126 (gcc.dg/torture/bitint-43.c, -44.c). */
#include <stdio.h>
#define U(T, v) do { T x = (v); \
    printf("%-22s %d %d %d %d %d %d\n", #T, __builtin_popcountg(x), __builtin_parityg(x), \
           x ? __builtin_clzg(x) : -1, x ? __builtin_ctzg(x) : -1, __builtin_clzg(x, -7), __builtin_ctzg(x, 99)); } while (0)
#define S(T, v) do { T x = (v); printf("%-22s %d %d\n", #T, __builtin_ffsg(x), __builtin_clrsbg(x)); } while (0)
#define ALLU(v) U(unsigned char, v); U(unsigned short, v); U(unsigned int, v); U(unsigned long, v); \
  U(unsigned long long, v); U(unsigned __int128, v); U(unsigned _BitInt(7), v); U(unsigned _BitInt(65), v); \
  U(unsigned _BitInt(129), v); U(unsigned _BitInt(575), v)
#define ALLS(v) S(signed char, v); S(short, v); S(int, v); S(long, v); S(long long, v); S(__int128, v); \
  S(_BitInt(7), v); S(_BitInt(65), v); S(_BitInt(129), v); S(_BitInt(575), v)
int main(void) {
  puts("-- 0"); ALLU(0);
  puts("-- 1"); ALLU(1);
  puts("-- all ones"); ALLU(-1);
  puts("-- 0x50"); ALLU(0x50);
  { unsigned _BitInt(575) h = 1; h <<= 574; U(unsigned _BitInt(575), h); U(unsigned _BitInt(575), h | 0x100); }
  { unsigned _BitInt(129) h = 3; h <<= 127; U(unsigned _BitInt(129), h); }
  { unsigned __int128 h = 1; h <<= 64; U(unsigned __int128, h); }
  { unsigned _BitInt(65) h = 1; h <<= 64; U(unsigned _BitInt(65), h); }
  puts("-- signed 0"); ALLS(0);
  puts("-- signed 1"); ALLS(1);
  puts("-- signed -1"); ALLS(-1);
  puts("-- signed -2"); ALLS(-2);
  puts("-- signed 0x30"); ALLS(0x30);
  puts("-- signed -0x31"); ALLS(-0x31);
  { _BitInt(575) h = 1; h <<= 400; S(_BitInt(575), h); S(_BitInt(575), -h); }
  /* the result is int; the second argument converted to int */
  printf("%d %d\n", (int)sizeof(__builtin_popcountg(0ull)), (int)sizeof(__builtin_clzg(0u, (char)5)));
  /* constant folding: gcc folds them in constant expressions */
  enum { E1 = __builtin_popcountg(0xffu), E2 = __builtin_clzg((unsigned short)1), E3 = __builtin_ctzg(0u, 40),
         E4 = __builtin_ffsg(-8), E5 = __builtin_clrsbg(-1ll), E6 = __builtin_parityg(7ul) };
  static int st[] = { __builtin_popcountg((unsigned _BitInt(200))-1), __builtin_clzg((unsigned _BitInt(200))1) };
  printf("%d %d %d %d %d %d %d %d\n", E1, E2, E3, E4, E5, E6, st[0], st[1]);
  return 0;
}

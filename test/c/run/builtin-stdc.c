/* gcc's builtins of C23's <stdbit.h> (__builtin_stdc_*, gcc 14; rotate_left and rotate_right gcc 15) over the
   unsigned types and _BitInt narrow and wide: their values at 0, 1, all ones, the top bit and a pattern, and their
   result types (unsigned int, bool, or the argument's type). Queue 126 (gcc.dg/bitint-106.c, -109.c, -121.c,
   gcc.dg/torture/bitint-75.c). */
#include <stdio.h>
#define P(x) (unsigned long long)((unsigned _BitInt(256))(x) >> 128), (unsigned long long)((unsigned _BitInt(256))(x) >> 64), (unsigned long long)(x)
#define C(T, v) do { T x = (v); \
    printf("%-22s lz%u lo%u tz%u to%u fl0%u fl1%u ft0%u ft1%u c0%u c1%u s%d w%u\n", #T, \
      __builtin_stdc_leading_zeros(x), __builtin_stdc_leading_ones(x), __builtin_stdc_trailing_zeros(x), \
      __builtin_stdc_trailing_ones(x), __builtin_stdc_first_leading_zero(x), __builtin_stdc_first_leading_one(x), \
      __builtin_stdc_first_trailing_zero(x), __builtin_stdc_first_trailing_one(x), __builtin_stdc_count_zeros(x), \
      __builtin_stdc_count_ones(x), (int)__builtin_stdc_has_single_bit(x), __builtin_stdc_bit_width(x)); \
    T f = __builtin_stdc_bit_floor(x), c = __builtin_stdc_bit_ceil(x), l = __builtin_stdc_rotate_left(x, 3), \
      r = __builtin_stdc_rotate_right(x, 3), l0 = __builtin_stdc_rotate_left(x, 0), lb = __builtin_stdc_rotate_left(x, 1000); \
    printf("  floor %llx.%llx.%llx ceil %llx.%llx.%llx rotl3 %llx.%llx.%llx rotr3 %llx.%llx.%llx rotl0 %llx rotlbig %llx\n", \
      P(f), P(c), P(l), P(r), (unsigned long long)l0, (unsigned long long)lb); } while (0)
#define ALL(v) C(unsigned char, v); C(unsigned short, v); C(unsigned int, v); C(unsigned long long, v); \
  C(unsigned __int128, v); C(unsigned _BitInt(7), v); C(unsigned _BitInt(65), v); C(unsigned _BitInt(129), v); \
  C(unsigned _BitInt(256), v)
int main(void) {
  puts("-- 0"); ALL(0);
  puts("-- 1"); ALL(1);
  puts("-- all ones"); ALL(-1);
  puts("-- 0x50"); ALL(0x50);
  puts("-- 5 (ceil 8)"); ALL(5);
  { unsigned _BitInt(129) h = 1; h <<= 128; C(unsigned _BitInt(129), h); C(unsigned _BitInt(129), h + 1); }
  { unsigned char h = 0x81; C(unsigned char, h); }
  /* the result types: unsigned int, bool, the argument's */
  printf("%d %d %d %d %d\n", (int)sizeof(__builtin_stdc_leading_zeros((unsigned char)0)),
         _Generic(__builtin_stdc_has_single_bit(0u), bool: 1, default: 0),
         _Generic(__builtin_stdc_bit_floor((unsigned short)5), unsigned short: 1, default: 0),
         _Generic(__builtin_stdc_rotate_left((unsigned _BitInt(9))5, 1), unsigned _BitInt(9): 1, default: 0),
         _Generic(__builtin_stdc_bit_width((unsigned _BitInt(300))5), unsigned int: 1, default: 0));
  /* a rotate count of another type, and a count wider than the width */
  printf("%x %x %x\n", (unsigned)__builtin_stdc_rotate_right((unsigned char)1, 9ull), (unsigned)__builtin_stdc_rotate_left(0x80000001u, (unsigned char)1),
         (unsigned)__builtin_stdc_rotate_left((unsigned _BitInt(7))0x41, 7));
  enum { E1 = __builtin_stdc_bit_width(1000u), E2 = __builtin_stdc_bit_ceil(9u), E3 = __builtin_stdc_rotate_left((unsigned char)0x81, 1),
         E4 = __builtin_stdc_first_leading_one(0u), E5 = __builtin_stdc_has_single_bit(64ull) };
  printf("%d %d %d %d %d\n", E1, E2, E3, E4, E5);
  return 0;
}

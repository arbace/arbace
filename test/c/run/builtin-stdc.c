/* ci-run verdict: exit 0. gcc's builtins of C23's <stdbit.h> (__builtin_stdc_*, gcc 14; rotate_left and rotate_right gcc 15) over the
   unsigned types and _BitInt narrow and wide: their values at 0, 1, all ones, the top bit and a pattern, and their
   result types (unsigned int, bool, or the argument's type). Queue 126 (gcc.dg/bitint-106.c, -109.c, -121.c,
   gcc.dg/torture/bitint-75.c). Checks itself: each value against C23's 7.18 definitions computed bit by bit (shifts of
   the value widened to unsigned _BitInt(575), not the builtins), bit_ceil of a value whose ceiling does not fit 0 as
   gcc's manual defines it ((type) 2 << ...), the types and constants by hand; a difference is reported on stderr and
   returns 1. */
#include <stdio.h>
typedef unsigned _BitInt(575) W_t;
static int fails;
static int bit(W_t x, int i) { return (int)((x >> i) & 1); }
static W_t one(int i) { return (W_t)1 << i; }
static W_t mask(int w) { return w == 575 ? ~(W_t)0 : one(w) - 1; }
static int lz(W_t x, int w) { int n = 0; for (int i = w - 1; i >= 0 && !bit(x, i); i--) n++; return n; }
static int tz(W_t x, int w) { int n = 0; for (int i = 0; i < w && !bit(x, i); i++) n++; return n; }
static int pop(W_t x, int w) { int n = 0; for (int i = 0; i < w; i++) n += bit(x, i); return n; }
static W_t rotl(W_t x, int w, unsigned long long n) { int k = (int)(n % (unsigned)w); W_t r = 0;
  for (int i = 0; i < w; i++) if (bit(x, i)) r |= one((i + k) % w); return r; }
static void want(const char *t, const char *what, int ok) { if (!ok) { fprintf(stderr, "FAIL %s %s\n", t, what); fails++; } }
static void check(const char *t, int w, W_t x, const unsigned *r, W_t f, W_t c, W_t l, W_t rr, W_t l0, W_t lb) {
  W_t nx = ~x & mask(w);
  int z = lz(x, w), o = lz(nx, w), tzs = tz(x, w), tos = tz(nx, w);
  want(t, "leading_zeros", r[0] == z); want(t, "leading_ones", r[1] == o);
  want(t, "trailing_zeros", r[2] == tzs); want(t, "trailing_ones", r[3] == tos);
  want(t, "first_leading_zero", r[4] == (o < w ? o + 1 : 0)); want(t, "first_leading_one", r[5] == (z < w ? z + 1 : 0));
  want(t, "first_trailing_zero", r[6] == (tos < w ? tos + 1 : 0)); want(t, "first_trailing_one", r[7] == (tzs < w ? tzs + 1 : 0));
  want(t, "count_zeros", r[8] == w - pop(x, w)); want(t, "count_ones", r[9] == pop(x, w));
  want(t, "has_single_bit", r[10] == (pop(x, w) == 1)); want(t, "bit_width", r[11] == w - z);
  want(t, "bit_floor", f == (x ? one(w - 1 - z) : 0));
  /* 2 << (prec - 1 - clzg (x - 1)) in the type: 0 when it does not fit */
  want(t, "bit_ceil", c == (x <= 1 ? 1 : w - lz(x - 1, w) >= w ? 0 : one(w - lz(x - 1, w))));
  want(t, "rotate_left 3", l == rotl(x, w, 3)); want(t, "rotate_right 3", rr == rotl(x, w, (unsigned)w - 3 % w));
  want(t, "rotate_left 0", l0 == x); want(t, "rotate_left 1000", lb == rotl(x, w, 1000)); }
#define P(x) (unsigned long long)((unsigned _BitInt(256))(x) >> 128), (unsigned long long)((unsigned _BitInt(256))(x) >> 64), (unsigned long long)(x)
#define C(T, W, v) do { T x = (v); \
    unsigned r_[12] = { __builtin_stdc_leading_zeros(x), __builtin_stdc_leading_ones(x), __builtin_stdc_trailing_zeros(x), \
      __builtin_stdc_trailing_ones(x), __builtin_stdc_first_leading_zero(x), __builtin_stdc_first_leading_one(x), \
      __builtin_stdc_first_trailing_zero(x), __builtin_stdc_first_trailing_one(x), __builtin_stdc_count_zeros(x), \
      __builtin_stdc_count_ones(x), (unsigned)__builtin_stdc_has_single_bit(x), __builtin_stdc_bit_width(x) }; \
    printf("%-22s lz%u lo%u tz%u to%u fl0%u fl1%u ft0%u ft1%u c0%u c1%u s%d w%u\n", #T, \
      r_[0], r_[1], r_[2], r_[3], r_[4], r_[5], r_[6], r_[7], r_[8], r_[9], (int)r_[10], r_[11]); \
    T f = __builtin_stdc_bit_floor(x), c = __builtin_stdc_bit_ceil(x), l = __builtin_stdc_rotate_left(x, 3), \
      r = __builtin_stdc_rotate_right(x, 3), l0 = __builtin_stdc_rotate_left(x, 0), lb = __builtin_stdc_rotate_left(x, 1000); \
    printf("  floor %llx.%llx.%llx ceil %llx.%llx.%llx rotl3 %llx.%llx.%llx rotr3 %llx.%llx.%llx rotl0 %llx rotlbig %llx\n", \
      P(f), P(c), P(l), P(r), (unsigned long long)l0, (unsigned long long)lb); \
    check(#T, W, (W_t)x, r_, f, c, l, r, l0, lb); } while (0)
#define ALL(v) C(unsigned char, 8, v); C(unsigned short, 16, v); C(unsigned int, 32, v); C(unsigned long long, 64, v); \
  C(unsigned __int128, 128, v); C(unsigned _BitInt(7), 7, v); C(unsigned _BitInt(65), 65, v); C(unsigned _BitInt(129), 129, v); \
  C(unsigned _BitInt(256), 256, v)
int main(void) {
  puts("-- 0"); ALL(0);
  puts("-- 1"); ALL(1);
  puts("-- all ones"); ALL(-1);
  puts("-- 0x50"); ALL(0x50);
  puts("-- 5 (ceil 8)"); ALL(5);
  { unsigned _BitInt(129) h = 1; h <<= 128; C(unsigned _BitInt(129), 129, h); C(unsigned _BitInt(129), 129, h + 1); }
  { unsigned char h = 0x81; C(unsigned char, 8, h); }
  /* the result types: unsigned int, bool, the argument's */
  int t_[5] = { (int)sizeof(__builtin_stdc_leading_zeros((unsigned char)0)),
         _Generic(__builtin_stdc_has_single_bit(0u), bool: 1, default: 0),
         _Generic(__builtin_stdc_bit_floor((unsigned short)5), unsigned short: 1, default: 0),
         _Generic(__builtin_stdc_rotate_left((unsigned _BitInt(9))5, 1), unsigned _BitInt(9): 1, default: 0),
         _Generic(__builtin_stdc_bit_width((unsigned _BitInt(300))5), unsigned int: 1, default: 0) };
  printf("%d %d %d %d %d\n", t_[0], t_[1], t_[2], t_[3], t_[4]);
  /* a rotate count of another type, and a count wider than the width */
  unsigned q_[3] = { (unsigned)__builtin_stdc_rotate_right((unsigned char)1, 9ull), (unsigned)__builtin_stdc_rotate_left(0x80000001u, (unsigned char)1),
         (unsigned)__builtin_stdc_rotate_left((unsigned _BitInt(7))0x41, 7) };
  printf("%x %x %x\n", q_[0], q_[1], q_[2]);
  enum { E1 = __builtin_stdc_bit_width(1000u), E2 = __builtin_stdc_bit_ceil(9u), E3 = __builtin_stdc_rotate_left((unsigned char)0x81, 1),
         E4 = __builtin_stdc_first_leading_one(0u), E5 = __builtin_stdc_has_single_bit(64ull) };
  printf("%d %d %d %d %d\n", E1, E2, E3, E4, E5);
  if (!(t_[0] == 4 && t_[1] && t_[2] && t_[3] && t_[4])) fails++;
  if (!(q_[0] == 0x80 && q_[1] == 3 && q_[2] == 0x41)) fails++;
  if (!(E1 == 10 && E2 == 16 && E3 == 3 && E4 == 0 && E5 == 1)) fails++;
  return fails ? 1 : 0;
}

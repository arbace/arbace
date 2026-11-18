/* ci-run verdict: exit 0. gcc's type-generic bit builtins (__builtin_popcountg, clzg, ctzg, ffsg, parityg, clrsbg; gcc 14), over every
   integer width ci has: the standard types, unsigned __int128 and _BitInt narrow and wide; clzg and ctzg with their
   second argument (the value for 0). Queue 126 (gcc.dg/torture/bitint-43.c, -44.c). Checks itself: each value against
   the builtin's definition in gcc's manual computed bit by bit (shifts of the value widened to unsigned _BitInt(575),
   not the builtins), and the constants' by hand; a difference is reported on stderr and returns 1. */
#include <stdio.h>
typedef unsigned _BitInt(575) W_t;
static int fails;
static int bit(W_t x, int i) { return (int)((x >> i) & 1); }
static int lz(W_t x, int w) { int n = 0; for (int i = w - 1; i >= 0 && !bit(x, i); i--) n++; return n; }
static int tz(W_t x, int w) { int n = 0; for (int i = 0; i < w && !bit(x, i); i++) n++; return n; }
static int pop(W_t x, int w) { int n = 0; for (int i = 0; i < w; i++) n += bit(x, i); return n; }
static void want(const char *t, const char *what, int got, int exp) {
  if (got != exp) { fprintf(stderr, "FAIL %s %s: %d, want %d\n", t, what, got, exp); fails++; } }
/* popcount, parity, clz and ctz (undefined at 0: -1 here), clzg (x, -7), ctzg (x, 99) */
static void ucheck(const char *t, int w, W_t x, const int *r) {
  int z = pop(x, w) == 0;
  want(t, "popcountg", r[0], pop(x, w)); want(t, "parityg", r[1], pop(x, w) & 1);
  want(t, "clzg", r[2], z ? -1 : lz(x, w)); want(t, "ctzg", r[3], z ? -1 : tz(x, w));
  want(t, "clzg2", r[4], z ? -7 : lz(x, w)); want(t, "ctzg2", r[5], z ? 99 : tz(x, w)); }
/* ffs: one plus the lowest set bit's index, 0 for 0; clrsb: the bits after the sign bit equal to it */
static void scheck(const char *t, int w, W_t x, const int *r) {
  int n = 0; for (int i = w - 2; i >= 0 && bit(x, i) == bit(x, w - 1); i--) n++;
  want(t, "ffsg", r[0], pop(x, w) ? tz(x, w) + 1 : 0); want(t, "clrsbg", r[1], n); }
#define U(T, W, v) do { T x = (v); \
    int r_[6] = { __builtin_popcountg(x), __builtin_parityg(x), x ? __builtin_clzg(x) : -1, x ? __builtin_ctzg(x) : -1, \
                  __builtin_clzg(x, -7), __builtin_ctzg(x, 99) }; \
    printf("%-22s %d %d %d %d %d %d\n", #T, r_[0], r_[1], r_[2], r_[3], r_[4], r_[5]); ucheck(#T, W, (W_t)x, r_); } while (0)
#define S(T, W, v) do { T x = (v); int r_[2] = { __builtin_ffsg(x), __builtin_clrsbg(x) }; \
    printf("%-22s %d %d\n", #T, r_[0], r_[1]); scheck(#T, W, (W_t)x, r_); } while (0)
#define ALLU(v) U(unsigned char, 8, v); U(unsigned short, 16, v); U(unsigned int, 32, v); U(unsigned long, 64, v); \
  U(unsigned long long, 64, v); U(unsigned __int128, 128, v); U(unsigned _BitInt(7), 7, v); U(unsigned _BitInt(65), 65, v); \
  U(unsigned _BitInt(129), 129, v); U(unsigned _BitInt(575), 575, v)
#define ALLS(v) S(signed char, 8, v); S(short, 16, v); S(int, 32, v); S(long, 64, v); S(long long, 64, v); S(__int128, 128, v); \
  S(_BitInt(7), 7, v); S(_BitInt(65), 65, v); S(_BitInt(129), 129, v); S(_BitInt(575), 575, v)
int main(void) {
  puts("-- 0"); ALLU(0);
  puts("-- 1"); ALLU(1);
  puts("-- all ones"); ALLU(-1);
  puts("-- 0x50"); ALLU(0x50);
  { unsigned _BitInt(575) h = 1; h <<= 574; U(unsigned _BitInt(575), 575, h); U(unsigned _BitInt(575), 575, h | 0x100); }
  { unsigned _BitInt(129) h = 3; h <<= 127; U(unsigned _BitInt(129), 129, h); }
  { unsigned __int128 h = 1; h <<= 64; U(unsigned __int128, 128, h); }
  { unsigned _BitInt(65) h = 1; h <<= 64; U(unsigned _BitInt(65), 65, h); }
  puts("-- signed 0"); ALLS(0);
  puts("-- signed 1"); ALLS(1);
  puts("-- signed -1"); ALLS(-1);
  puts("-- signed -2"); ALLS(-2);
  puts("-- signed 0x30"); ALLS(0x30);
  puts("-- signed -0x31"); ALLS(-0x31);
  { _BitInt(575) h = 1; h <<= 400; S(_BitInt(575), 575, h); S(_BitInt(575), 575, -h); }
  /* the result is int; the second argument converted to int */
  printf("%d %d\n", (int)sizeof(__builtin_popcountg(0ull)), (int)sizeof(__builtin_clzg(0u, (char)5)));
  /* constant folding: gcc folds them in constant expressions */
  enum { E1 = __builtin_popcountg(0xffu), E2 = __builtin_clzg((unsigned short)1), E3 = __builtin_ctzg(0u, 40),
         E4 = __builtin_ffsg(-8), E5 = __builtin_clrsbg(-1ll), E6 = __builtin_parityg(7ul) };
  static int st[] = { __builtin_popcountg((unsigned _BitInt(200))-1), __builtin_clzg((unsigned _BitInt(200))1) };
  printf("%d %d %d %d %d %d %d %d\n", E1, E2, E3, E4, E5, E6, st[0], st[1]);
  if (sizeof(__builtin_popcountg(0ull)) != sizeof(int) || sizeof(__builtin_clzg(0u, (char)5)) != sizeof(int)) fails++;
  if (!(E1 == 8 && E2 == 15 && E3 == 40 && E4 == 4 && E5 == 63 && E6 == 1 && st[0] == 200 && st[1] == 199)) fails++;
  return fails ? 1 : 0;
}

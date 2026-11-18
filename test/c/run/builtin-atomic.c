/* ci-run verdict: exit 0. gcc's __atomic_* and __sync_* builtins (LLS21 (a): the lowering of LLVM IR writes them): the _n and the generic forms,
   every read-modify-write at several widths, compare-exchange both ways, test-and-set, the fences, and fmodf and fmodl.
   One thread: the memory orders change nothing. Each call is its own statement (the arguments' order is unspecified).
   Checks itself: the values printed, in order, against those gcc's manual defines for each builtin (the fetch_ forms
   the old value, the _fetch forms the new, nand ~(a & b), compare-exchange's result and expected value, test_and_set
   the old state) worked out by hand from the initial values, and fmod's (C's: the remainder with the dividend's sign);
   a difference returns 1. Arbace's own, EPL-1.0. */
#include <stdio.h>
#include <math.h>
static long long seen[64]; static int nseen;
#define S(x) do { long long r_ = (long long)(x); printf("%lld ", r_); seen[nseen++] = r_; } while (0)
#define V(x) do { long long r_ = (long long)(x); printf("[%lld]\n", r_); seen[nseen++] = r_; } while (0)
static const long long want[] = { 5, 5, 8, 10, 10, 8, 8, 8, 8, 8, 11, 11, 14, 14, -7, -1, 3, 11, 10, 10, 250, 4,
  1099511627776LL, 1099511627777LL, -7, -7, 10, 77, 1, 9, 77, 0, 9, 9, 40, 40, 1, 40, 1, 2, 1, 0, 1, 0, 1, 2, 12, 12,
  12, 1, 100, 100, 4, 0 };
int main(void) {
  int i = 5; unsigned char c = 250; long long l = 1LL << 40; short s = -3; void *p = &i; _Bool b = 0;
  S(__atomic_load_n(&i, __ATOMIC_SEQ_CST)); S(__atomic_fetch_add(&i, 3, __ATOMIC_RELAXED)); V(i);
  S(__atomic_add_fetch(&i, 2, __ATOMIC_ACQ_REL)); S(__atomic_fetch_sub(&i, 1, __ATOMIC_SEQ_CST));
  S(__atomic_sub_fetch(&i, 1, __ATOMIC_SEQ_CST)); V(i);
  S(__atomic_fetch_and(&i, 12, 5)); V(i);
  S(__atomic_fetch_or(&i, 3, 5)); V(i);
  S(__atomic_fetch_xor(&i, 5, 5)); V(i);
  S(__atomic_fetch_nand(&i, 6, 5)); V(i);
  S(__atomic_nand_fetch(&i, 6, 5)); S(__atomic_and_fetch(&i, 3, 5)); S(__atomic_or_fetch(&i, 8, 5)); S(__atomic_xor_fetch(&i, 1, 5)); V(i);
  S(__atomic_fetch_add(&c, 10, 5)); V(c);
  S(__atomic_fetch_add(&l, 1, 5)); V(l);
  S(__atomic_add_fetch(&s, -4, 5)); V(s);
  S(__atomic_exchange_n(&i, 77, 5)); V(i);
  int e = 77, d = 1, o, v = 40;
  S(__atomic_compare_exchange_n(&i, &e, 9, 0, 5, 5)); S(i); V(e);
  e = 3;
  S(__atomic_compare_exchange_n(&i, &e, 11, 0, 5, 5)); S(i); V(e);
  __atomic_store_n(&i, 8, 5); __atomic_store(&i, &v, 5); __atomic_load(&i, &o, 5);
  S(i); V(o);
  __atomic_exchange(&i, &d, &o, 5);
  S(i); V(o);
  e = 1; d = 2;
  S(__atomic_compare_exchange(&i, &e, &d, 0, 5, 5)); S(i); V(e);
  S(__atomic_test_and_set(&b, 5)); S(__atomic_test_and_set(&b, 5)); __atomic_clear(&b, 5); V(b);
  void *q = 0;
  __atomic_store_n(&q, p, 5);
  V(q == p);
  __atomic_thread_fence(5); __atomic_signal_fence(5);
  S(__sync_fetch_and_add(&i, 5)); S(__sync_add_and_fetch(&i, 5)); V(i);
  S(__sync_val_compare_and_swap(&i, 12, 1)); S(__sync_bool_compare_and_swap(&i, 1, 100)); V(i);
  S(__sync_lock_test_and_set(&i, 4)); V(i);
  __sync_lock_release(&i); __sync_synchronize();
  V(i);
  printf("%g %g %g\n", fmodf(7.5f, 2.0f), (double)fmodl(-7.5L, 2.0L), __builtin_fmod(7.5, 2.0));
  printf("%g %g\n", (double)__builtin_fmodf(-9.25f, 4.0f), (double)__builtin_fmodl(9.25L, -4.0L));
  if (nseen != (int)(sizeof want / sizeof want[0])) return 1;
  for (int k = 0; k < nseen; k++) if (seen[k] != want[k]) return 1;
  if (fmodf(7.5f, 2.0f) != 1.5f || fmodl(-7.5L, 2.0L) != -1.5L || __builtin_fmod(7.5, 2.0) != 1.5
      || __builtin_fmodf(-9.25f, 4.0f) != -1.25f || __builtin_fmodl(9.25L, -4.0L) != 1.25L) return 1;
  return 0;
}

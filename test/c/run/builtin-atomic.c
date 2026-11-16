/* gcc's __atomic_* and __sync_* builtins (LLS21 (a): the lowering of LLVM IR writes them): the _n and the generic forms,
   every read-modify-write at several widths, compare-exchange both ways, test-and-set, the fences, and fmodf and fmodl.
   One thread: the memory orders change nothing. Each call is its own statement (the arguments' order is unspecified).
   Arbace's own, EPL-1.0. */
#include <stdio.h>
#include <math.h>
#define S(x) do { long long r_ = (long long)(x); printf("%lld ", r_); } while (0)
#define V(x) printf("[%lld]\n", (long long)(x))
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
  return 0;
}

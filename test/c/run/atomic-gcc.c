/* ci-run verdict: exit 0. gcc's __atomic builtins as CPython's pyatomic_gcc.h calls them (the Mac VM's report 269, finding
   2: the checker refused the generic __atomic_load and __atomic_store, "too many arguments to function", and the builder had
   no __atomic_fetch_add): through static inline functions of an included header, at the widths 1 to 8, on const-qualified
   and pointer-sized objects, float and double through the generic forms, compare-exchange both ways, and fetch-add on a
   pointer unscaled ("as if the operands were of the uintptr_t type", extend.texi, "__atomic Builtins"). One thread: the
   memory orders change nothing. The program checks itself against extend.texi's semantics and aborts on a difference.
   Arbace's own, EPL-1.0. */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
static int fails;
#define EQ(x, v) do { if ((long long)(x) != (long long)(v)) { printf("FAIL %s: %lld, want %lld\n", #x, (long long)(x), (long long)(v)); fails++; } } while (0)
#include "atomic-gcc.h"
int main(void) {
  int i = 1; int8_t c = 126; uint16_t u = 65535; int64_t l = INT64_C(1) << 40; intptr_t ip = -5;
  uint32_t m = 0xff0fu; uint8_t b = 0x81; long lr = 77;
  EQ(add_int(&i, 4), 1); EQ(i, 5);
  EQ(add_int8(&c, 3), 126); EQ(c, -127);
  EQ(add_uint16(&u, 2), 65535); EQ(u, 1);
  EQ(add_int64(&l, -1), INT64_C(1) << 40); EQ(l, (INT64_C(1) << 40) - 1);
  EQ(add_intptr(&ip, 10), -5); EQ(ip, 5);
  EQ(and_uint32(&m, 0x0ff0u), 0xff0fu); EQ(m, 0x0f00u);
  EQ(or_uint8(&b, 0x0e), 0x81); EQ(b, 0x8f);
  int e = 5;
  EQ(cas_int(&i, &e, 9), 1); EQ(i, 9); EQ(e, 5);
  e = 3;
  EQ(cas_int(&i, &e, 11), 0); EQ(i, 9); EQ(e, 9);
  EQ(exchange_int(&i, 21), 9); EQ(i, 21);
  store_int(&i, 33); EQ(load_int(&i), 33);
  EQ(load_long_relaxed(&lr), 77);
  int x = 1, y = 2; void *p = &x, *ex = &x;
  EQ(cas_ptr(&p, &ex, &y), 1); EQ(p == (void *)&y, 1);
  EQ(cas_ptr(&p, &ex, &x), 0); EQ(ex == (void *)&y, 1);
  EQ(exchange_ptr(&p, &x) == (void *)&y, 1); EQ(p == (void *)&x, 1);
  store_ptr_release(&p, &y); EQ(load_ptr_acquire(&p) == (void *)&y, 1);
  float f = 1.5f; double d = 0;
  EQ(load_float_relaxed(&f) == 1.5f, 1);
  store_float_relaxed(&f, -2.25f); EQ(f == -2.25f, 1);
  store_double_relaxed(&d, 6.125); EQ(load_double_relaxed(&d) == 6.125, 1);
  fence_seq_cst();
  int arr[4] = { 0, 1, 2, 3 }; int *q = &arr[0];
  int *old = __atomic_fetch_add(&q, sizeof(int), __ATOMIC_SEQ_CST);
  EQ(old == &arr[0], 1); EQ(q == &arr[1], 1);
  EQ(*__atomic_add_fetch(&q, 2 * sizeof(int), __ATOMIC_SEQ_CST), 3);
  EQ(__atomic_sub_fetch(&q, sizeof(int), __ATOMIC_SEQ_CST) == &arr[2], 1);
  printf("i %d c %d u %u l %lld ip %ld m %x b %x f %g d %g\n", i, c, u, (long long)l, (long)ip, m, b, f, d);
  printf("atomic-gcc: %d failing\n", fails);
  fflush(stdout);
  if (fails) abort();
  return 0;
}

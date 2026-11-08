// G5c: GNU's __atomic builtins (clang's AtomicExpr, CGAtomic's -O0 forms): load, store, exchange, compare-exchange
// (strong and weak, the failure order folded), fetch-and-op and op-and-fetch, test-and-set, clear; constant orders and
// run-time ones (clang's switch over the C ABI's values); libsupc++'s guard.cc, new_handler.cc and atomicity.h use them
#include <cstdio>
static int g = 5;
static long gl = 1;
static int* gp = nullptr;
static int load_at(int* p, int o) { return __atomic_load_n(p, o); }
static void store_at(int* p, int v, int o) { __atomic_store_n(p, v, o); }
static int add_at(int* p, int v, int o) { return __atomic_fetch_add(p, v, o); }
static bool cas_at(int* p, int* e, int d, int s, int f) { return __atomic_compare_exchange_n(p, e, d, false, s, f); }
int main() {
  printf("%d\n", __atomic_load_n(&g, __ATOMIC_ACQUIRE));
  __atomic_store_n(&g, 7, __ATOMIC_RELEASE);
  printf("%d %d\n", __atomic_exchange_n(&g, 9, __ATOMIC_ACQ_REL), g);
  int e = 3;
  bool ok = __atomic_compare_exchange_n(&g, &e, 11, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
  printf("%d %d %d\n", ok, e, g);
  ok = __atomic_compare_exchange_n(&g, &e, 11, true, __ATOMIC_SEQ_CST, __ATOMIC_RELAXED);
  printf("%d %d %d\n", ok, e, g);
  printf("%d %d\n", __atomic_fetch_add(&g, 2, __ATOMIC_ACQ_REL), __atomic_add_fetch(&g, 2, __ATOMIC_SEQ_CST));
  printf("%d %d\n", __atomic_fetch_sub(&g, 1, __ATOMIC_RELAXED), __atomic_sub_fetch(&g, 1, __ATOMIC_RELAXED));
  printf("%d %d %d\n", __atomic_fetch_and(&g, 6, __ATOMIC_SEQ_CST), __atomic_or_fetch(&g, 16, __ATOMIC_SEQ_CST), __atomic_xor_fetch(&g, 3, __ATOMIC_SEQ_CST));
  printf("%d %d\n", __atomic_nand_fetch(&g, 12, __ATOMIC_SEQ_CST), g);
  printf("%ld\n", __atomic_fetch_add(&gl, 41L, __ATOMIC_SEQ_CST) + __atomic_load_n(&gl, __ATOMIC_SEQ_CST));
  int x = 1;
  __atomic_store_n(&gp, &x, __ATOMIC_RELEASE);
  printf("%d\n", *__atomic_load_n(&gp, __ATOMIC_ACQUIRE));
  int v = 0, r = 0;
  __atomic_load(&g, &r, __ATOMIC_SEQ_CST); v = r + 1; __atomic_store(&g, &v, __ATOMIC_SEQ_CST);
  printf("%d %d\n", r, g);
  char flag = 0;
  printf("%d %d\n", __atomic_test_and_set(&flag, __ATOMIC_SEQ_CST), __atomic_test_and_set(&flag, __ATOMIC_SEQ_CST));
  __atomic_clear(&flag, __ATOMIC_SEQ_CST);
  printf("%d\n", flag);
  int h = 0;
  for (int o = 0; o <= 5; ++o) { store_at(&h, o, o); printf("%d", load_at(&h, o)); }
  printf(" %d %d\n", add_at(&h, 3, 4), h);
  int ee = 8;
  printf("%d %d %d\n", cas_at(&h, &ee, 1, 5, 2), ee, h);
  printf("%d %d %d\n", cas_at(&h, &ee, 1, 4, 0), ee, h);
  return 0;
}

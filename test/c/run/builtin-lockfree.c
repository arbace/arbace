/* gcc's __atomic_is_lock_free and __atomic_always_lock_free (gcc 4.7), and C11's atomic_is_lock_free over them
   (<stdatomic.h>): sizes 1, 2, 4 and 8, with a null pointer (the size's typical alignment) and with objects; the
   sizes whose answer is the target's (16 and up) are left out of the printed lines. Queue 126
   (gcc.dg/atomic/c23-stdatomic-lockfree-char8_t.c, gnu23-stdatomic-lockfree-char8_t.c). */
#include <stdio.h>
#include <stdatomic.h>
int main(void) {
  _Atomic char c; _Atomic short s; _Atomic int i; _Atomic long long ll; atomic_char8_t c8; _Atomic(void *) p;
  printf("%d %d %d %d\n", __atomic_is_lock_free(1, 0), __atomic_is_lock_free(2, 0), __atomic_is_lock_free(4, 0), __atomic_is_lock_free(8, 0));
  printf("%d %d %d %d\n", __atomic_always_lock_free(1, 0), __atomic_always_lock_free(2, 0), __atomic_always_lock_free(4, 0), __atomic_always_lock_free(8, 0));
  printf("%d %d %d %d %d %d\n", atomic_is_lock_free(&c), atomic_is_lock_free(&s), atomic_is_lock_free(&i), atomic_is_lock_free(&ll),
         atomic_is_lock_free(&c8), atomic_is_lock_free(&p));
  printf("%d %d %d\n", __atomic_is_lock_free(sizeof i, &i), ATOMIC_CHAR8_T_LOCK_FREE, ATOMIC_LLONG_LOCK_FREE);
  printf("%d %d\n", (int)sizeof(__atomic_is_lock_free(1, 0)), _Generic(__atomic_is_lock_free(1, 0), bool: 1, default: 0));
  enum { E1 = __atomic_always_lock_free(4, 0), E2 = __atomic_always_lock_free(3, 0) };
  printf("%d %d\n", E1, E2);
  return 0;
}

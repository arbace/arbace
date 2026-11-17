/* A call through a function pointer reads the result at the pointer's return type (LLS21 (a), abi-punning): a function
   that returns unsigned called as int (*)(), as qsort's comparator is, is read as the same register, so -1 stays -1 and
   sorts first; likewise a long returned and read as int, and an int read as unsigned char. gcc -O0 reads eax. Arbace's own, EPL-1.0. */
#include <stdio.h>
#include <stdlib.h>
static unsigned cmp_u(const void *a, const void *b) { int x = *(const int *)a, y = *(const int *)b; return (unsigned)(x < y ? -1 : x > y); }
static long wide(void) { return 0x100000005L; }
static int neg(void) { return -2; }
int main(void) {
  int v[] = {3, -1, 7, 0, -9, 2};
  qsort(v, 6, sizeof v[0], (int (*)(const void *, const void *))cmp_u);
  for (int i = 0; i < 6; i++) printf("%d ", v[i]);
  printf("\n%d %u\n", ((int (*)(void))wide)(), ((unsigned char (*)(void))neg)());
  return 0;
}

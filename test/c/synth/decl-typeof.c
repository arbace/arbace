// synth-c23/decl-typeof.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// typeof and typeof_unqual of expressions and types
// ISO/IEC 9899:2024 6.7.3.6; Annex A: typeof-specifier, typeof-specifier-argument
// M.2: typeof and typeof_unqual
#include <stdio.h>

static int add(int a, int b) { return a + b; }

int main(void) {
  const int ci = 5;
  typeof(ci) t1 = 6;
  typeof_unqual(ci) t2 = 7;
  t2++;
  typeof(int[3]) arr = {1, 2, 3};
  typeof(add) *fp = add;
  typeof(typeof(char *)[2]) sp = {"a", "b"};
  volatile double vd = 1;
  typeof_unqual(vd) plain = 2;
  typeof(1 + 1.0f) f = 0.5f;
  _BitInt(9) b9 = 3;
  typeof(b9) b9b = b9 + 1;
  printf("%s = %d\n", "t1", t1);
  printf("%s = %d\n", "t2", t2);
  printf("%s = %d\n", "_Generic(&t1, const int *: 1, default: 0)", _Generic(&t1, const int *: 1, default: 0));
  printf("%s = %d\n", "_Generic(&t2, int *: 1, default: 0)", _Generic(&t2, int *: 1, default: 0));
  printf("%s = %d\n", "arr[2]", arr[2]);
  printf("%s = %d\n", "fp(2, 3)", fp(2, 3));
  printf("%s = %s\n", "sp[1]", sp[1]);
  printf("%s = %d\n", "_Generic(&plain, double *: 1, default: 0)", _Generic(&plain, double *: 1, default: 0));
  printf("%s = %d\n", "_Generic(f, float: 1, default: 0)", _Generic(f, float: 1, default: 0));
  printf("%s = %d\n", "(int)b9b", (int)b9b);
  printf("%s = %zu\n", "sizeof(typeof(arr)) / sizeof(int)", sizeof(typeof(arr)) / sizeof(int));
  return 0;
}

// synth-c23/decl-empty-initializer.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23's empty initializer {} for scalars, arrays, structures, unions and VLAs
// ISO/IEC 9899:2024 6.7.11; Annex A: braced-initializer ({ })
// M.2: empty initializer {}
#include <stdio.h>

struct s { int a; double b; int *p; };
static struct s fs = {};

int main(void) {
  int i = {};
  int a[3] = {};
  struct s st = {};
  union { int x; char c; } un = {};
  int n = 3;
  int v[n] = {};
  int *p = {};
  printf("%s = %d\n", "i", i);
  printf("%s = %d\n", "a[0] + a[2]", a[0] + a[2]);
  printf("%s = %d\n", "st.a", st.a);
  printf("%s = %g\n", "st.b", st.b);
  printf("%s = %d\n", "st.p == nullptr", st.p == nullptr);
  printf("%s = %d\n", "un.x", un.x);
  printf("%s = %d\n", "v[0] + v[2]", v[0] + v[2]);
  printf("%s = %d\n", "p == nullptr", p == nullptr);
  printf("%s = %d\n", "fs.a", fs.a);
  return 0;
}

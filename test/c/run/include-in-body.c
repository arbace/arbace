/* ci-run verdict: exit 0. #include inside function bodies (the Mac VM's report 269, finding 2: ci refused
   _testcapimodule.c's #include "_testcapi_feature_macros.inc" inside get_feature_macros as split-declaration): statements
   with conditional groups, a table inside a local initializer, declarations and a #pragma pack, in a nested block and in a
   loop. gcc's #include replaces the directive by the file's tokens wherever it stands (cpp.texi, "Include Operation"). The
   program checks itself and aborts on a difference. Arbace's own, EPL-1.0. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
static int fails;
#define EQ(x, v) do { if ((long)(x) != (long)(v)) { printf("FAIL %s: %ld, want %ld\n", #x, (long)(x), (long)(v)); fails++; } } while (0)
#define HAVE_TWO 1
static int seen[2];
static int add(int *result, const char *name, int v) { *result += v; seen[strcmp(name, "HAVE_ONE") == 0 ? 0 : 1] = v + 1; return 0; }
static int feature_macros(int *result) {
  int res;
#include "include-in-body.inc"
  return 0;
}
static int table_sum(void) {
  static const int tab[] = {
#include "include-in-body-tab.inc"
  };
  int s = 0;
  for (unsigned i = 0; i < sizeof tab / sizeof tab[0]; i++) s += tab[i];
  return s * 100 + (int)(sizeof tab / sizeof tab[0]);
}
int main(void) {
  int result = 0;
  EQ(feature_macros(&result), 0);
  EQ(result, 1); EQ(seen[0], 1); EQ(seen[1], 2);
  EQ(table_sum(), 3905);
  int n = 0;
  for (int k = 0; k < 2; k++) {
#include "include-in-body-decl.inc"
    n += inc_local;
    EQ(sizeof(struct IncPacked), 5);
  }
  struct After { char c; int i; };
  EQ(n, 84); EQ(sizeof(struct After), 8);
  printf("features %d %d %d table %d decls %d\n", result, seen[0], seen[1], table_sum(), n);
  printf("include-in-body: %d failing\n", fails);
  fflush(stdout);
  if (fails) abort();
  return 0;
}

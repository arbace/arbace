// synth-c23/pp-has-attribute.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// __has_c_attribute in #if: the standard attributes (202311), prefixed vendor ones, unknown ones (0)
// ISO/IEC 9899:2024 6.10.2; Annex A: has-c-attribute-express
// M.2: __has_c_attribute
#include <stdio.h>

#if __has_c_attribute(deprecated) == 202311L
static int a0 = 1;
#else
static int a0 = 0;
#endif
#if __has_c_attribute(fallthrough) == 202311L
static int a1 = 1;
#else
static int a1 = 0;
#endif
#if __has_c_attribute(maybe_unused) == 202311L
static int a2 = 1;
#else
static int a2 = 0;
#endif
#if __has_c_attribute(nodiscard) == 202311L
static int a3 = 1;
#else
static int a3 = 0;
#endif
#if __has_c_attribute(noreturn) == 202311L
static int a4 = 1;
#else
static int a4 = 0;
#endif
#if __has_c_attribute(unsequenced) == 202311L
static int a5 = 1;
#else
static int a5 = 0;
#endif
#if __has_c_attribute(reproducible) == 202311L
static int a6 = 1;
#else
static int a6 = 0;
#endif
#if __has_c_attribute(__nodiscard__) == 202311L
static int a7 = 1;
#else
static int a7 = 0;
#endif
#if __has_c_attribute(_Noreturn) == 202311L
static int a8 = 1;
#else
static int a8 = 0;
#endif
#if __has_c_attribute(gnu::packed) == 1
static int a9 = 1;
#else
static int a9 = 0;
#endif
#if __has_c_attribute(foo) == 0
static int a10 = 1;
#else
static int a10 = 0;
#endif
#if __has_c_attribute(x) == 0
static int a11 = 1;
#else
static int a11 = 0;
#endif
#if defined(__has_c_attribute) && __has_c_attribute(nodiscard) && !__has_c_attribute(no_such_attribute)
static int in_if = 1;
#else
static int in_if = 0;
#endif

int main(void) {
  printf("%s = %d\n", "a0", a0);
  printf("%s = %d\n", "a1", a1);
  printf("%s = %d\n", "a2", a2);
  printf("%s = %d\n", "a3", a3);
  printf("%s = %d\n", "a4", a4);
  printf("%s = %d\n", "a5", a5);
  printf("%s = %d\n", "a6", a6);
  printf("%s = %d\n", "a7", a7);
  printf("%s = %d\n", "a8", a8);
  printf("%s = %d\n", "a9", a9);
  printf("%s = %d\n", "a10", a10);
  printf("%s = %d\n", "a11", a11);
  printf("%s = %d\n", "in_if", in_if);
  return 0;
}

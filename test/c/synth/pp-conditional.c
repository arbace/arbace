// synth-c23/pp-conditional.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// conditional inclusion: #if, #elif, #else, defined, #ifdef, #ifndef, C23's #elifdef and #elifndef, true and false
// ISO/IEC 9899:2024 6.10.2; Annex A: if-section, if-group, elif-groups, elif-group, else-group, endif-line
// M.2: #elifdef and #elifndef; true and false in #if
#include <stdio.h>

#define ONE 1
#if ONE + 1 == 2 && defined(ONE) && defined ONE
static int a = 1;
#else
static int a = 0;
#endif
#if 0
#error never
#elif ONE * 3 > 2
static int b = 2;
#else
static int b = 0;
#endif
#ifdef NOPE
static int c = 0;
#elifdef ONE
static int c = 3;
#else
static int c = -1;
#endif
#ifndef ONE
static int d = 0;
#elifndef NOPE
static int d = 4;
#endif
#if true && !false
static int e = 5;
#endif
#if 'A' == 65 && (-1 < 0u) == 0 && 0x10 == 16 && (2 ? 3 : 4) == 3
static int f = 6;
#endif
#if __STDC_VERSION__ >= 202311L
static int g = 7;
#endif
#if 1'000 == 1000 && 0b11 == 3
static int h = 8;
#endif
#if UNDEFINED_NAME == 0
static int i = 9;
#endif

int main(void) {
  printf("%s = %d\n", "a", a);
  printf("%s = %d\n", "b", b);
  printf("%s = %d\n", "c", c);
  printf("%s = %d\n", "d", d);
  printf("%s = %d\n", "e", e);
  printf("%s = %d\n", "f", f);
  printf("%s = %d\n", "g", g);
  printf("%s = %d\n", "h", h);
  printf("%s = %d\n", "i", i);
  return 0;
}

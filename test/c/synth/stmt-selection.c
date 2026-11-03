// synth-c23/stmt-selection.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// selection statements: if, else, chains, dangling else, switch with fallthrough
// ISO/IEC 9899:2024 6.8.5; Annex A: selection-statement
// M.2: the fallthrough attribute
#include <stdio.h>

static int sign(int v) { if (v < 0) return -1; else if (v == 0) return 0; else return 1; }
static int dangling(int a, int b) { if (a) if (b) return 1; else return 2; return 3; }
static int fall(int v) {
  int r = 0;
  switch (v) {
  case 1: r += 1; [[fallthrough]];
  case 2: r += 2; [[fallthrough]];
  case 3: r += 3; break;
  case 4: { r = 40; break; }
  }
  return r;
}
static int nested(int a, int b) { switch (a) { case 0: switch (b) { case 0: return 0; default: return 1; } default: return 2; } }

int main(void) {
  int t = 0;
  if (1) t = 1;
  if (0) t = 2; else t += 2;
  printf("%s = %d\n", "t", t);
  printf("%s = %d\n", "sign(-5)", sign(-5));
  printf("%s = %d\n", "sign(0)", sign(0));
  printf("%s = %d\n", "sign(9)", sign(9));
  printf("%s = %d\n", "dangling(1, 0)", dangling(1, 0));
  printf("%s = %d\n", "dangling(0, 1)", dangling(0, 1));
  printf("%s = %d\n", "fall(1)", fall(1));
  printf("%s = %d\n", "fall(2)", fall(2));
  printf("%s = %d\n", "fall(4)", fall(4));
  printf("%s = %d\n", "fall(5)", fall(5));
  printf("%s = %d\n", "nested(0, 0)", nested(0, 0));
  printf("%s = %d\n", "nested(0, 3)", nested(0, 3));
  printf("%s = %d\n", "nested(1, 0)", nested(1, 0));
  return 0;
}

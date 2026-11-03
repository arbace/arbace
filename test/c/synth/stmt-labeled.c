// synth-c23/stmt-labeled.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// labeled statements: labels, case and default, goto forward and backward
// ISO/IEC 9899:2024 6.8.2; Annex A: labeled-statement, label
#include <stdio.h>

static int classify(int v) {
  switch (v) {
  case 0: return 10;
  case 1:
  case 2: return 20;
  default: return 30;
  case 3: return 40;
  }
}

int main(void) {
  int n = 0;
  back:
  n++;
  if (n < 3) goto back;
  goto fwd;
  n = 100;
  fwd:
  n += 10;
  printf("%s = %d\n", "n", n);
  printf("%s = %d\n", "classify(0)", classify(0));
  printf("%s = %d\n", "classify(2)", classify(2));
  printf("%s = %d\n", "classify(3)", classify(3));
  printf("%s = %d\n", "classify(9)", classify(9));
  return 0;
}

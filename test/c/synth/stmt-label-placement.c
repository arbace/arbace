// synth-c23/stmt-label-placement.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23's labels before declarations and at the end of compound statements
// ISO/IEC 9899:2024 6.8.2, 6.8.3; Annex A: labeled-statement, block-item (label)
// M.2: labels before declarations and at the end of compound statements
#include <stdio.h>

int main(void) {
  int r = 0;
  goto decl;
  decl:
  int x = 5;
  r += x;
  switch (r) {
  case 5:
  int y = 2;
  r += 1;
  break;
  default:
  }
  { r++; end: }
  if (r == 7) goto done;
  r = -1;
  done:
  printf("%s = %d\n", "r", r);
  return 0;
}

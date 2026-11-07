// synth-c23/stmt-duff.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// a switch whose case labels are inside a loop (Duff's device)
// ISO/IEC 9899:2024 6.8.5.3, 6.8.2; Annex A: selection-statement, labeled-statement, iteration-statement
#include <stdio.h>

static int duff(const int *from, int count) {
  int sum = 0, n = (count + 3) / 4;
  switch (count % 4) {
  case 0: do { sum += *from++;
  case 3: sum += *from++;
  case 2: sum += *from++;
  case 1: sum += *from++;
  } while (--n > 0);
  }
  return sum;
}

int main(void) {
  int a[7] = {1, 2, 3, 4, 5, 6, 7};
  printf("%s = %d\n", "duff(a, 7)", duff(a, 7));
  printf("%s = %d\n", "duff(a, 4)", duff(a, 4));
  printf("%s = %d\n", "duff(a, 1)", duff(a, 1));
  return 0;
}

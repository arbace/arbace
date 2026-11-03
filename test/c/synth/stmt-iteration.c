// synth-c23/stmt-iteration.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// iteration statements: while, do, for with and without declarations, empty clauses
// ISO/IEC 9899:2024 6.8.6; Annex A: iteration-statement
#include <stdio.h>

int main(void) {
  int w = 0, i = 0;
  while (i < 4) w += i++;
  int d = 0;
  do d += 2; while (d < 7);
  int f = 0;
  for (int k = 0; k < 5; k++) f += k;
  int j;
  for (j = 10; j > 0; j -= 3) ;
  int inf = 0;
  for (;;) { if (++inf == 6) break; }
  int skip = 0;
  for (int a = 0, b = 10; a < b; a++, b--) { if (a % 2) continue; skip += a; }
  int outer = 0;
  for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) { if (y > x) break; outer++; }
  printf("%s = %d\n", "w", w);
  printf("%s = %d\n", "d", d);
  printf("%s = %d\n", "f", f);
  printf("%s = %d\n", "j", j);
  printf("%s = %d\n", "inf", inf);
  printf("%s = %d\n", "skip", skip);
  printf("%s = %d\n", "outer", outer);
  return 0;
}

// synth-c23/stmt-jump.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// jump statements: goto, continue, break, return with and without a value
// ISO/IEC 9899:2024 6.8.7; Annex A: jump-statement
#include <stdio.h>

static int calls;
static void noop(int x) { if (x) return; calls++; }
static int find(const int *a, int n, int v) { for (int i = 0; i < n; i++) { if (a[i] != v) continue; return i; } return -1; }

int main(void) {
  int a[4] = {5, 6, 7, 8};
  noop(0);
  noop(1);
  int g = 0;
  loop: if (g < 4) { g += 2; goto loop; }
  int br = 0;
  while (1) { if (++br > 3) break; }
  printf("%s = %d\n", "calls", calls);
  printf("%s = %d\n", "find(a, 4, 7)", find(a, 4, 7));
  printf("%s = %d\n", "find(a, 4, 9)", find(a, 4, 9));
  printf("%s = %d\n", "g", g);
  printf("%s = %d\n", "br", br);
  return 0;
}

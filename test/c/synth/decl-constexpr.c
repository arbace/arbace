// synth-c23/decl-constexpr.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// constexpr objects: integers, floating, structures, arrays, use in constant expressions
// ISO/IEC 9899:2024 6.7.2, 6.6; Annex A: storage-class-specifier (constexpr)
// M.2: constexpr objects
#include <stdio.h>

struct pt { int x, y; };
static constexpr int N = 4;
constexpr double HALF = 0.5;
constexpr struct pt ORIGIN = {1, 2};
static int buf[N * 2];
static constexpr unsigned char BYTES[3] = {1, 2, 3};
static_assert(N == 4 && ORIGIN.y == 2);

int main(void) {
  constexpr int local = N + 1;
  int arr[local];
  constexpr bool yes = true;
  constexpr char c = 'q';
  printf("%s = %d\n", "N", N);
  printf("%s = %g\n", "HALF", HALF);
  printf("%s = %d\n", "ORIGIN.x + ORIGIN.y", ORIGIN.x + ORIGIN.y);
  printf("%s = %zu\n", "sizeof buf / sizeof buf[0]", sizeof buf / sizeof buf[0]);
  printf("%s = %zu\n", "sizeof arr / sizeof arr[0]", sizeof arr / sizeof arr[0]);
  printf("%s = %d\n", "yes", yes);
  printf("%s = %d\n", "c", c);
  printf("%s = %d\n", "BYTES[2]", BYTES[2]);
  printf("%s = %d\n", "_Generic(N, int: 1, default: 0)", _Generic(N, int: 1, default: 0));
  return 0;
}

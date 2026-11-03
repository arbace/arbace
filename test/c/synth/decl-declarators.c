// synth-c23/decl-declarators.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// declarators: pointers, arrays, functions and their compositions
// ISO/IEC 9899:2024 6.7.7; Annex A: declarator, direct-declarator, array-declarator, function-declarator, pointer
#include <stdio.h>

static int one(void) { return 1; }
static int two(void) { return 2; }
static int (*pick(int i))(void) { return i ? two : one; }
static int (*table[2])(void) = {one, two};
static int grid[2][3] = {{1, 2, 3}, {4, 5, 6}};

int main(void) {
  int x = 3;
  int *p = &x;
  int **pp = &p;
  int *arr[2] = {&x, p};
  int (*row)[3] = grid;
  int (*(*fpp))(void) = &table[1];
  int *const *cpp = &p;
  printf("%s = %d\n", "**pp", **pp);
  printf("%s = %d\n", "*arr[1]", *arr[1]);
  printf("%s = %d\n", "row[1][2]", row[1][2]);
  printf("%s = %d\n", "pick(0)()", pick(0)());
  printf("%s = %d\n", "pick(1)()", pick(1)());
  printf("%s = %d\n", "table[0]()", table[0]());
  printf("%s = %d\n", "(*fpp)()", (*fpp)());
  printf("%s = %d\n", "**cpp", **cpp);
  printf("%s = %d\n", "(*(row + 1))[0]", (*(row + 1))[0]);
  return 0;
}

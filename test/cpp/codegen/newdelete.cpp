// new and delete: objects, arrays of scalars, arrays of classes with destructors (the cookie), initializers
#include <cstdio>
struct Cell { int v; Cell() : v(-1) { printf("Cell()\n"); } ~Cell() { printf("~Cell %d\n", v); } };
struct Plain { int a, b; };
int main() {
  int *p = new int(5);
  int *q = new int;
  *q = *p + 1;
  printf("%d %d\n", *p, *q);
  delete p; delete q;
  int *arr = new int[10];
  for (int i = 0; i < 10; i++) arr[i] = i * i;
  printf("%d\n", arr[9]);
  delete[] arr;
  Cell *cells = new Cell[3];
  for (int i = 0; i < 3; i++) cells[i].v = i;
  delete[] cells;
  Plain *pl = new Plain{3, 4};
  printf("%d %d\n", pl->a, pl->b);
  delete pl;
  int *z = new int[4]();
  printf("%d %d\n", z[0], z[3]);
  delete[] z;
  Cell *one = new Cell;
  one->v = 99;
  delete one;
  int *none = nullptr;
  delete none;
  return 0;
}

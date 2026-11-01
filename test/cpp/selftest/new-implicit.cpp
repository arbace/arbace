typedef int A[10];
struct B { int x; };
typedef B C[3];
void f() {
  int *p = new A;
  B *q = new C;
  int (*r)[5] = new int[2][5];
  delete[] r;
}
// re-review 032 F1: the array size of a new expression is decided by its text: `T[]` (C++20's deduced size, clang's literal is its
// count) and a macro size of type unsigned long keep their array; the forms print back as written
#define N 3UL
struct S { int a; };
void g() {
  int *e1 = new int[]{1, 2, 3};
  int *e2 = new int[]{5};
  int *e3 = new int[N];
  S *e4 = new S[]{{1}, {2}};
  char *e5 = new char[]{"abc"};
  int (*e6)[2] = new int[][2]{{1, 2}, {3, 4}};
}

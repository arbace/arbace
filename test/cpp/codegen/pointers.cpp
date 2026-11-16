// arrays, pointer arithmetic, pointers to pointers, function pointers, 2D arrays
#include <cstdio>
int add(int a, int b) { return a + b; }
int mul(int a, int b) { return a * b; }
int apply(int (*op)(int, int), int a, int b) { return op(a, b); }
void fill(int *p, int n, int v) { for (int i = 0; i < n; i++) *p++ = v + i; }
int main() {
  int a[5] = {1, 2, 3};
  int *p = a, *q = &a[4];
  printf("%ld %d %d\n", q - p, *(p + 1), p[2]);
  fill(a, 5, 10);
  for (int i = 0; i < 5; i++) printf("%d ", a[i]);
  printf("\n");
  int m[3][4];
  for (int i = 0; i < 3; i++) for (int j = 0; j < 4; j++) m[i][j] = i * 10 + j;
  printf("%d %d %ld\n", m[2][3], *(*(m + 1) + 2), sizeof m);
  int x = 5, *px = &x, **ppx = &px;
  **ppx = 42;
  printf("%d\n", x);
  int (*ops[2])(int, int) = {add, mul};
  printf("%d %d %d\n", ops[0](3, 4), ops[1](3, 4), apply(&add, 1, 2));
  const char *words[] = {"alpha", "beta", "gamma"};
  for (const char **w = words; w != words + 3; ++w) printf("%s %c\n", *w, (*w)[1]);
  int *np = nullptr;
  printf("%d %d\n", np == nullptr, p != nullptr);
  char buf[8] = "abc";
  printf("%s %d %d\n", buf, buf[3], (int)sizeof buf);
  return 0;
}

typedef int A[10];
struct B { int x; };
typedef B C[3];
void f() {
  int *p = new A;
  B *q = new C;
  int (*r)[5] = new int[2][5];
  delete[] r;
}

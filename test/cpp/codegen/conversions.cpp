// implicit conversions: converting constructors, derived-to-base pointers and references, bool, null pointers, void*
#include <cstdio>
struct Meters { double v; Meters(double x) : v(x) {} };
double twice(Meters m) { return m.v * 2; }
struct A { int a = 1; };
struct B : A { int b = 2; };
int getA(const A &x) { return x.a; }
int getA(const A *x) { return x ? x->a : -1; }
int main() {
  printf("%.1f %.1f\n", twice(3.5), twice(2));
  B b;
  A *pa = &b;
  printf("%d %d %d\n", getA(b), getA(pa), getA(nullptr));
  void *vp = &b;
  B *pb = static_cast<B *>(vp);
  printf("%d\n", pb->b);
  int i = 3.99; char c = 300; unsigned u = -1; bool f = 0.0, t = 5;
  printf("%d %d %u %d %d\n", i, c, u, f, t);
  double d = 'a'; long l = 2.5e9; float fl = 1e10;
  printf("%.1f %ld %.0f\n", d, l, (double)fl);
  if (pa) printf("non-null\n");
  int *ip = 0;
  printf("%d\n", !ip);
  return 0;
}

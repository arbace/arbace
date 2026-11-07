// constructors: mem-initializers, default member initializers, delegation, copy constructors, member objects
#include <cstdio>
struct Inner { int v; Inner() : v(1) { printf("Inner() %d\n", v); } Inner(int x) : v(x) { printf("Inner(%d)\n", x); } };
struct Outer {
  int a = 7;
  Inner in;
  Inner in2{5};
  double d;
  Outer() : d(0.5) { printf("Outer() %d %.1f\n", a, d); }
  Outer(int x) : a(x), in(x * 2), d(1) { printf("Outer(%d)\n", x); }
  Outer(int x, int y) : Outer(x + y) { printf("delegated %d\n", a); }
};
struct Copy { int n; Copy(int k) : n(k) {} Copy(const Copy &o) : n(o.n + 100) { printf("copy %d\n", o.n); } };
Copy make(int k) { Copy c(k); return c; }
void take(Copy c) { printf("took %d\n", c.n); }
int main() {
  Outer o1;
  Outer o2(3);
  Outer o3(1, 2);
  printf("%d %d %d\n", o1.in.v, o2.in.v, o3.in2.v);
  Copy c1(1);
  Copy c2 = c1;
  Copy c3(c2);
  take(c3);
  Copy c4 = make(9);
  printf("%d %d %d %d\n", c1.n, c2.n, c3.n, c4.n);
  return 0;
}

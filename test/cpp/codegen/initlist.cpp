// G5: std::initializer_list: an initializer-list constructor chosen first ([over.match.list]/1.1), its backing array,
// a function taking initializer_list<int>, nested braces in the second phase ([over.best.ics]/4: no copy constructor
// through a user-defined conversion), an array of a non-aggregate class from braces, a list of class elements whose
// array is destroyed at the full-expression's end or with a declared initializer_list
#include <initializer_list>
#include <cstdio>
struct A { int i, j; A(int a, int b) : i(a), j(b) {} };
struct B { A a; B(A x) : a(x) {} };
struct C { B b; C(B x) : b(x) {} };
struct V { int n, s; V(std::initializer_list<int> l) : n(int(l.size())), s(0) { for (int x : l) s += x; } V(int a, int b) : n(-a), s(-b) {} };
struct D { int k; D(int x) : k(x) { printf("D%d\n", k); } D(const D& o) : k(o.k) { printf("copy D%d\n", k); } ~D() { printf("~D%d\n", k); } };
static int total(std::initializer_list<int> l) { int t = 0; for (const int* p = l.begin(); p != l.end(); ++p) t += *p; return t; }
static int count(std::initializer_list<D> l) { return int(l.size()); }
void f(C c) { printf("%d %d\n", c.b.a.i, c.b.a.j); }
void f(int);
struct U { int u; U(int a, int b) : u(a * 10 + b) {} };
U ua[] = { { 3, 2 }, { 4, 1 } };
int main() {
  V v{1, 2, 3, 4};
  V w(5, 6);
  printf("%d %d %d %d\n", v.n, v.s, w.n, w.s);
  printf("%d\n", total({10, 20, 30}));
  f({{{1, 2}}});
  f({{A{3, 4}}});
  printf("%d %d\n", ua[0].u, ua[1].u);
  printf("%d\n", count({D(1), D(2)}));
  { std::initializer_list<D> l = {D(7)}; printf("in scope %d\n", int(l.size())); }
  printf("end\n");
  return 0;
}

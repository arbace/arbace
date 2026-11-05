// temporaries: destroyed at the end of their full-expression, in conditional branches, as arguments, extended by a reference
#include <cstdio>
struct T {
  int v;
  T(int x) : v(x) { printf("T(%d)\n", v); }
  T(const T &o) : v(o.v) { printf("copy T(%d)\n", v); }
  ~T() { printf("~T(%d)\n", v); }
  int get() const { return v; }
  T plus(int k) const { return T(v + k); }
};
int use(const T &t) { return t.get(); }
int byval(T t) { return t.get() * 10; }
int main() {
  printf("a %d\n", T(1).get() + T(2).get());
  printf("b %d\n", use(T(3)));
  printf("c %d\n", byval(T(4)));
  bool flag = true;
  printf("d %d\n", flag ? T(5).get() : T(6).get());
  printf("e %d\n", T(7).plus(1).plus(1).get());
  const T &r = T(10);
  printf("f %d\n", r.get());
  T x = T(20);
  printf("g %d\n", (flag && use(T(30)) > 0) ? x.get() : 0);
  return 0;
}

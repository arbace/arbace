// lambdas in member functions: capturing this and calling member functions, a lambda stored in a local and passed as a
// function pointer to a static member function, a lambda in a constructor initializing a member, a lambda in an
// inline member function (the Ul names)
#include <cstdio>
struct Counter {
  int n;
  int limit;
  Counter(int lim) : n(0), limit([lim]() { return lim > 10 ? 10 : lim; }()) {}
  bool full() const { return n >= limit; }
  int fill() { auto step = [this](int k) { while (!full() && k--) n++; return n; }; step(3); return step(100); }
  static int apply(int (*f)(int), int v) { return f(v); }
  int twice() const { return apply([](int x) { return 2 * x; }, n); }
};
int main() {
  Counter a(25), b(4);
  printf("%d %d\n", a.limit, b.limit);
  printf("%d %d\n", a.fill(), b.fill());
  printf("%d %d\n", a.twice(), b.twice());
  return 0;
}

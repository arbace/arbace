// exceptions and lambdas together: a throw from a lambda's body, a lambda called inside try, a capture destroyed on
// the unwind path, a handler inside a lambda
#include <cstdio>
struct Res { int id; Res(int i) : id(i) {} Res(const Res& o) : id(o.id) {} ~Res() { printf("~Res %d\n", id); } };
int main() {
  Res r(1);
  auto risky = [r](int x) { if (x > 2) throw x * 10; return x + r.id; };
  for (int i = 1; i <= 4; i++) {
    try { printf("ok %d\n", risky(i)); } catch (int e) { printf("lambda threw %d\n", e); }
  }
  auto safe = [](int x) -> int { try { if (x) throw x; return 0; } catch (int v) { return -v; } };
  printf("%d %d\n", safe(0), safe(7));
  try { Res inner(2); auto f = [&inner]() { throw inner; }; f(); } catch (const Res& e) { printf("caught Res %d\n", e.id); }
  return 0;
}

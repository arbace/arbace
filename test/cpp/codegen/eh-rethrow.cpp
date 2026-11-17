// exceptions: rethrow with throw;, nested try blocks, a throw from a handler, the caught object kept alive until its handler ends
#include <cstdio>
struct E { int n; E(int n) : n(n) { printf("E(%d)\n", n); } E(const E& o) : n(o.n) { printf("E copy(%d)\n", n); } ~E() { printf("~E(%d)\n", n); } };
void f(int k) {
  try {
    throw E(k);
  } catch (E& e) {
    printf("inner %d\n", e.n);
    e.n += 10;
    if (k == 1) throw;
    if (k == 2) throw E(k * 100);
  }
  printf("f(%d) normal\n", k);
}
int main() {
  for (int k = 0; k < 3; k++) {
    try { f(k); } catch (E e) { printf("outer %d\n", e.n); }
  }
  try {
    try { throw 1; } catch (int) { printf("first\n"); throw; }
  } catch (int i) { printf("again %d\n", i); }
  return 0;
}

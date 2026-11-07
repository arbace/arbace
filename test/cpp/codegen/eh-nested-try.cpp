// exceptions: try blocks nested in one function, an inner handler that does not match passes the exception out,
// handlers in loops with break and continue, a try inside a handler
#include <cstdio>
struct A { int v; }; struct B { int v; };
int classify(int i) {
  try {
    try {
      if (i == 0) throw A{1};
      if (i == 1) throw B{2};
      if (i == 2) throw 3;
      return 0;
    } catch (A& a) { return 10 + a.v; }
  } catch (B& b) {
    try { throw b.v * 100; } catch (int x) { return x; }
  } catch (...) { return -1; }
}
int main() {
  for (int i = 0; i < 4; i++) printf("%d -> %d\n", i, classify(i));
  int sum = 0;
  for (int i = 0; i < 10; i++) {
    try { if (i % 3 == 0) throw i; sum += i; }
    catch (int x) { if (x == 9) break; continue; }
    sum += 100;
  }
  printf("sum %d\n", sum);
  return 0;
}

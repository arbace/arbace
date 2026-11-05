// exceptions: noexcept functions and destructors are nounwind, calls through function pointers inside try, a return inside a handler
#include <cstdio>
int twice(int x) noexcept { return 2 * x; }
int risky(int x) { if (x % 2) throw x; return x; }
int apply(int (*f)(int), int x) { try { return f(x); } catch (int e) { printf("apply caught %d\n", e); return -1; } }
struct Guard { const char* s; ~Guard() { printf("guard %s\n", s); } };
int loop() {
  int total = 0;
  for (int i = 0; i < 6; i++) {
    Guard g{"iter"};
    try { total += risky(i); } catch (int e) { if (e == 5) return total; continue; }
    total += twice(i);
  }
  return total;
}
int main() {
  printf("%d %d\n", apply(risky, 4), apply(risky, 3));
  printf("%d\n", apply(twice, 21));
  printf("loop %d\n", loop());
  return 0;
}

// exceptions: temporaries of a full-expression and objects of conditional branches destroyed on the unwind path
#include <cstdio>
struct S { int v; S(int v) : v(v) { printf("S(%d)\n", v); } ~S() { printf("~S(%d)\n", v); } S operator+(const S& o) const { if (v + o.v > 10) throw v + o.v; return S(v + o.v); } };
int pick(bool c, int a) { return (c ? S(a) : S(a + 1)).v; }
int main() {
  try { S r = S(1) + S(2) + S(3); printf("r %d\n", r.v); } catch (int e) { printf("caught %d\n", e); }
  try { S r = S(4) + S(5) + S(6); printf("r %d\n", r.v); } catch (int e) { printf("caught %d\n", e); }
  try { int x = pick(true, 3) + (S(7) + S(8)).v; printf("x %d\n", x); } catch (int e) { printf("caught %d\n", e); }
  return 0;
}

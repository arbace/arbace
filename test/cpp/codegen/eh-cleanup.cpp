// exceptions: the destructors of locals and temporaries run as the exception leaves their scopes, inner first
#include <cstdio>
struct T { int id; T(int i) : id(i) { printf("T(%d)\n", id); } T(const T& o) : id(o.id + 100) { printf("T copy %d\n", id); } ~T() { printf("~T(%d)\n", id); } int get() const { return id; } };
int boom(int x) { if (x == 3) throw x; return x; }
int use(const T& t, int x) { return t.get() + boom(x); }
void level2(int x) { T a(20); { T b(21); printf("sum %d\n", use(T(22), x)); } T c(23); boom(x + 1); }
void level1(int x) { T a(10); level2(x); printf("level1 done\n"); }
int main() {
  for (int x = 1; x <= 3; x++) {
    try { level1(x); } catch (int e) { printf("caught %d\n", e); }
  }
  return 0;
}

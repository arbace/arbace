// static locals: initialized once on first pass (a class with a destructor registered at exit), counters across calls
#include <cstdio>
struct Res { int id; Res(int i) : id(i) { printf("acquire %d\n", id); } ~Res() { printf("release %d\n", id); } };
int seq() { static int n = 0; return ++n; }
Res &get(int which) {
  if (which == 0) { static Res a(seq()); return a; }
  static Res b(100 + seq());
  return b;
}
int main() {
  for (int i = 0; i < 3; i++) printf("%d ", seq());
  printf("\n");
  printf("%d %d %d %d\n", get(1).id, get(0).id, get(1).id, get(0).id);
  return 0;
}

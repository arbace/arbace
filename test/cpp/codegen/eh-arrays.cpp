// exceptions: a throw while an array of objects is in scope destroys its elements in reverse; members of an
// aggregate; an exception out of a member function of a local object
#include <cstdio>
struct Tracked { int id; Tracked(int i = 0) : id(i) {} ~Tracked() { printf("~Tracked %d\n", id); } void check() const { if (id == 13) throw id; } };
struct Box { Tracked a; Tracked b; };
void run(int bad) {
  Tracked row[3] = {Tracked(1), Tracked(2), Tracked(3)};
  Box box{Tracked(10), Tracked(bad)};
  printf("built\n");
  box.b.check();
  printf("checked\n");
}
int main() {
  run(11);
  try { run(13); } catch (int e) { printf("caught %d\n", e); }
  return 0;
}

// exceptions: a derived class caught by its base's handler, polymorphic exception objects, handlers tried in order
#include <cstdio>
struct Base { virtual ~Base() {} virtual const char* name() const { return "Base"; } };
struct Mid : Base { int v; Mid(int v) : v(v) {} const char* name() const override { return "Mid"; } };
struct Leaf : Mid { Leaf() : Mid(9) {} const char* name() const override { return "Leaf"; } };
struct Other {};
void thrower(int i) {
  switch (i) { case 0: throw Base(); case 1: throw Mid(4); case 2: throw Leaf(); case 3: throw Other(); default: break; }
}
int main() {
  for (int i = 0; i < 5; i++) {
    try {
      thrower(i);
      printf("%d: nothing\n", i);
    } catch (Leaf& l) {
      printf("%d: Leaf handler %s %d\n", i, l.name(), l.v);
    } catch (Mid& m) {
      printf("%d: Mid handler %s %d\n", i, m.name(), m.v);
    } catch (Base& b) {
      printf("%d: Base handler %s\n", i, b.name());
    } catch (...) {
      printf("%d: other\n", i);
    }
  }
  return 0;
}

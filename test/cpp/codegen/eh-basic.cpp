// exceptions: throw and catch of int and of a class, catch by reference, by value and catch-all, the handlers' order
#include <cstdio>
struct Err { int code; const char* what; };
int check(int x) { if (x < 0) throw Err{x, "negative"}; if (x == 0) throw 0; if (x > 100) throw 2.5; return x * 2; }
int main() {
  int vals[] = {5, -3, 0, 200, 7};
  for (int v : vals) {
    try {
      printf("check(%d) = %d\n", v, check(v));
    } catch (const Err& e) {
      printf("Err %d %s\n", e.code, e.what);
    } catch (int i) {
      printf("int %d\n", i);
    } catch (...) {
      printf("something else\n");
    }
  }
  try { throw 'c'; } catch (char c) { printf("char %c\n", c); }
  try { throw "text"; } catch (const char* s) { printf("string %s\n", s); }
  return 0;
}

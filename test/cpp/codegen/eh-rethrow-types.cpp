// exceptions: pointers thrown and caught (to a class and to const char), a derived class pointer caught as a base
// pointer, enums thrown, catch(...) then rethrow to a typed handler, an exception escaping a destructor's caller
#include <cstdio>
enum class Code { Ok, Bad, Worse };
struct Base { virtual ~Base() {} virtual int v() const { return 1; } };
struct Der : Base { int v() const override { return 2; } };
static Der global;
void thrower(int i) {
  switch (i) { case 0: throw &global; case 1: throw "message"; case 2: throw Code::Worse; case 3: throw 2.5f; default: break; }
}
int main() {
  for (int i = 0; i < 5; i++) {
    try {
      try { thrower(i); printf("%d none\n", i); }
      catch (...) { printf("%d rethrowing\n", i); throw; }
    }
    catch (Base* b) { printf("Base* %d\n", b->v()); }
    catch (const char* s) { printf("text %s\n", s); }
    catch (Code c) { printf("code %d\n", (int)c); }
    catch (float f) { printf("float %.1f\n", f); }
  }
  return 0;
}

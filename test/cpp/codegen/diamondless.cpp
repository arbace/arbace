// multiple inheritance: three dynamic bases, an overrider of two bases' functions at once, a base with its own
// secondary base (nested secondary vtables), virtual calls during construction through each base
#include <cstdio>
struct A { virtual ~A() {} virtual int id() const { return 1; } virtual int a() const { return 10; } };
struct B { virtual ~B() {} virtual int id() const { return 2; } virtual int b() const { return 20; } };
struct C { int pad[3] = {7, 8, 9}; virtual ~C() {} virtual int c() const { return pad[2]; } };
struct BC : B, C { BC() { printf("BC sees %d\n", id()); } int c() const override { return 33; } };
struct D : A, BC {
  int x = 5;
  D() { printf("D sees %d\n", id()); }
  int id() const override { return 4 + x; }
  int b() const override { return 200 + x; }
};
int main() {
  D d;
  A& a = d; B& b = d; C& c = d; BC& bc = d;
  printf("%d %d %d %d\n", a.id(), b.id(), bc.id(), d.id());
  printf("%d %d %d %d\n", a.a(), b.b(), c.c(), bc.c());
  C* pc = &d;
  printf("%d\n", static_cast<D*>(static_cast<BC*>(pc))->x);
  A* heap = new D;
  delete heap;
  B* hb = new D;
  printf("%d\n", hb->id());
  delete hb;
  return 0;
}

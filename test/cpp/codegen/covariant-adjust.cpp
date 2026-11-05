#include <cstdio>
struct A { int a = 1; virtual ~A() {} };
struct B { int b = 2; virtual ~B() {} };
struct C : A, B { int c = 3; };
struct P { virtual B* get() { return 0; } virtual ~P() {} };
struct Q : P { C obj; C* get() override { return &obj; } };
int main() {
  Q q;
  P* p = &q;
  B* b = p->get();
  C* c = q.get();
  printf("%d %d %d\n", b->b, c->c, (void*)b == (void*)c);
  P* n = new P; B* nb = n->get(); printf("%d\n", nb == 0); delete n;
  return 0;
}

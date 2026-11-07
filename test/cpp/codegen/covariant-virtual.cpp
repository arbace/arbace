// covariant returns through a virtual base and by reference, and a further override of a covariant one
#include <cstdio>
struct V { int v = 7; virtual ~V() {} };
struct W : virtual V { int w = 8; };
struct X : virtual V { int x = 9; };
struct Y : W, X { int y = 10; };
struct P { virtual V* get() { return 0; } virtual V& ref() { static V s; return s; } virtual ~P() {} };
struct Q : P { Y obj; Y* get() override { return &obj; } Y& ref() override { return obj; } };
struct R : Q { Y obj2; Y* get() override { return &obj2; } };
int main() {
  R r; Q* q = &r; P* p = &r;
  V* a = p->get(); V* b = q->get(); V& c = p->ref();
  printf("%d %d %d %d\n", a->v, b->v, c.v, a == static_cast<V*>(r.get()));
  Y* y = r.get(); y->y = 11; printf("%d %d\n", y->y, r.obj2.y);
  P pp; printf("%d\n", pp.get() == 0);
  return 0;
}

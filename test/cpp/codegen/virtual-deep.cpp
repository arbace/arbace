// virtual inheritance in depth: a virtual base of a virtual base, a class with virtual and non-virtual bases, members
// with destructors in classes with virtual bases, dynamic_cast and typeid across virtual bases, pointer conversions
#include <cstdio>
#include <typeinfo>
struct Tracer { const char* n; Tracer(const char* n) : n(n) { printf("+%s\n", n); } ~Tracer() { printf("-%s\n", n); } };
struct Root { Tracer t{"root"}; int v = 1; virtual ~Root() {} virtual int get() const { return v; } };
struct Mid : virtual Root { Tracer t{"mid"}; int m = 10; int get() const override { return v + m; } };
struct Side { int s = 100; virtual ~Side() {} virtual int side() const { return s; } };
struct Top : virtual Mid, Side { Tracer t{"top"}; int x = 1000; int get() const override { return Mid::get() + x; } };
struct Leaf : Top { int side() const override { return -s; } };
int main() {
  {
    Leaf leaf;
    Root* r = &leaf;
    Mid* m = &leaf;
    Side* sd = &leaf;
    printf("%d %d %d %d\n", r->get(), m->get(), sd->side(), leaf.get());
    printf("%s %s\n", typeid(*r).name(), typeid(*sd).name());
    Leaf* back = dynamic_cast<Leaf*>(r);
    Side* across = dynamic_cast<Side*>(r);
    printf("%d %d %d\n", back == &leaf, across == sd, dynamic_cast<Top*>(m) != nullptr);
    Root* nr = nullptr; Mid* nm = nullptr;
    printf("null %d %d\n", (Root*)nm == nr, dynamic_cast<Mid*>(nr) == nullptr);
  }
  Root* heap = new Leaf;
  printf("%d\n", heap->get());
  delete heap;
  return 0;
}

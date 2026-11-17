// virtual inheritance of an interface without data (a nearly empty virtual base, the primary base of its first
// derived class), implementations mixed in from two sides, calls through the interface, a virtual base reached twice
#include <cstdio>
struct Shape { virtual ~Shape() {} virtual double area() const = 0; virtual const char* kind() const { return "shape"; } };
struct Colored : virtual Shape { int color = 3; const char* kind() const override { return "colored"; } };
struct Sized : virtual Shape { double w = 2, h = 5; double area() const override { return w * h; } };
struct Box : Colored, Sized { const char* kind() const override { return "box"; } };
struct Big : Box { double area() const override { return 2 * Box::area(); } };
double total(Shape* const* s, int n) { double t = 0; for (int i = 0; i < n; i++) t += s[i]->area(); return t; }
int main() {
  Box b; Big g;
  Shape* all[] = {&b, &g, static_cast<Sized*>(&g), static_cast<Colored*>(&b)};
  for (Shape* s : all) printf("%s %.1f\n", s->kind(), s->area());
  printf("total %.1f\n", total(all, 4));
  Colored& c = g;
  printf("%d %s\n", c.color, c.kind());
  printf("%zu %zu %zu %zu\n", sizeof(Colored), sizeof(Sized), sizeof(Box), sizeof(Big));
  Shape* heap = new Big;
  printf("%.1f\n", heap->area());
  delete heap;
  return 0;
}

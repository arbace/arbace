// a class template with virtual functions: its vtable and every virtual member instantiated with the class
#include <cstdio>
struct Shape { virtual ~Shape() {} virtual double area() const = 0; virtual const char* name() const { return "shape"; } };
template <class T> struct Square : Shape { T s; Square(T x) : s(x) {} double area() const { return (double)(s * s); } const char* name() const { return "square"; } };
int main() {
  Shape* a = new Square<int>(3);
  Shape* b = new Square<double>(1.5);
  printf("%s %g %s %g\n", a->name(), a->area(), b->name(), b->area());
  delete a; delete b;
  return 0;
}

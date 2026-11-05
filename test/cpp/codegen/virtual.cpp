// virtual functions: overriding, calls through base pointers and references, virtual destructors, pure virtuals
#include <cstdio>
struct Shape {
  virtual ~Shape() { printf("~Shape\n"); }
  virtual double area() const = 0;
  virtual const char *name() const { return "shape"; }
  void describe() const { printf("%s %.2f\n", name(), area()); }
};
struct Circle : Shape {
  double r;
  Circle(double r) : r(r) {}
  ~Circle() override { printf("~Circle\n"); }
  double area() const override { return 3.0 * r * r; }
  const char *name() const override { return "circle"; }
};
struct Square : Shape {
  double s;
  Square(double s) : s(s) {}
  double area() const override { return s * s; }
};
struct Tiny : Square { Tiny() : Square(0.5) {} const char *name() const override { return "tiny"; } };
void show(const Shape &s) { s.describe(); }
int main() {
  Shape *shapes[3] = {new Circle(1), new Square(2), new Tiny};
  for (Shape *s : shapes) s->describe();
  show(*shapes[2]);
  double total = 0;
  for (int i = 0; i < 3; i++) total += shapes[i]->area();
  printf("total %.2f\n", total);
  for (Shape *s : shapes) delete s;
  Circle c(2);
  Shape &ref = c;
  printf("%s %.1f\n", ref.name(), ref.Shape::name()[0] == 's' ? 1.0 : 0.0);
  return 0;
}

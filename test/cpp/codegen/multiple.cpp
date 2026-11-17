// multiple inheritance: two dynamic bases, the secondary vtable with this-adjusting thunks, calls and deletes through
// either base, pointer conversions with their offsets (null kept null), a non-dynamic base beside dynamic ones
#include <cstdio>
struct Named { const char* name; Named(const char* n) : name(n) {} };
struct Shape { int sides; Shape(int s) : sides(s) {} virtual ~Shape() { printf("~Shape\n"); } virtual double area() const = 0; virtual int corners() const { return sides; } };
struct Printable { int width = 8; virtual ~Printable() { printf("~Printable\n"); } virtual void print() const { printf("printable\n"); } virtual int w() const { return width; } };
struct Square : Named, Shape, Printable {
  double side;
  Square(double s) : Named("square"), Shape(4), side(s) {}
  ~Square() override { printf("~Square %s\n", name); }
  double area() const override { return side * side; }
  void print() const override { printf("%s of side %.1f, %d sides, width %d\n", name, side, sides, width); }
};
void show(const Printable& p) { p.print(); printf("w %d\n", p.w()); }
double measure(const Shape* s) { return s ? s->area() : -1; }
int main() {
  Square* sq = new Square(3);
  Shape* sh = sq;
  Printable* pr = sq;
  printf("%.1f %d\n", sh->area(), sh->corners());
  show(*sq);
  pr->print();
  printf("same %d %d\n", (void*)pr != (void*)sh, static_cast<Square*>(pr) == sq);
  Square* none = nullptr;
  Printable* pn = none;
  printf("null %d %.1f\n", pn == nullptr, measure(nullptr));
  delete pr;
  Square local(2);
  Shape& r = local;
  printf("%.1f\n", r.area());
  return 0;
}

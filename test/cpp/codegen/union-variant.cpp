// unions: a hand-made variant with a tag and an anonymous union holding a class with a constructor, union members
// read through pointers, a union with an array, unions in arrays
#include <cstdio>
#include <cstring>
struct Shape {
  enum { Circle, Rect } kind;
  union { struct { double r; } circle; struct { double w, h; } rect; };
  double area() const { return kind == Circle ? 3.0 * circle.r * circle.r : rect.w * rect.h; }
};
union Bits { double d; unsigned long long u; unsigned char b[8]; };
int main() {
  Shape shapes[3];
  shapes[0].kind = Shape::Circle; shapes[0].circle.r = 2;
  shapes[1].kind = Shape::Rect; shapes[1].rect.w = 3; shapes[1].rect.h = 4;
  shapes[2].kind = Shape::Circle; shapes[2].circle.r = 1;
  double total = 0;
  for (const Shape& s : shapes) total += s.area();
  printf("%.1f size %zu\n", total, sizeof(Shape));
  Bits x; x.d = 1.0;
  printf("%llx %d\n", x.u, x.b[7]);
  Bits* px = &x;
  px->u = 0x4000000000000000ull;
  printf("%.1f\n", px->d);
  Bits y; std::memcpy(&y, &x, sizeof y);
  printf("%.1f\n", y.d);
  return 0;
}

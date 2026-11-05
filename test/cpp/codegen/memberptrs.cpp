// pointers to members: data members (offsets, null as -1, through a derived object), member functions (non-virtual
// and virtual, called by .* and ->*), arrays of them, comparisons and null, conversion to bool
#include <cstdio>
struct Point { int x, y, z; int sum() const { return x + y + z; } int scale(int k) { x *= k; y *= k; z *= k; return sum(); } };
struct Base { int pad = 1; virtual ~Base() {} virtual int who() const { return 1; } int plain() const { return 10; } };
struct Derived : Base { int extra = 5; int who() const override { return 2; } };
int pick(const Point& p, int Point::*m) { return p.*m; }
int main() {
  Point pt{1, 2, 3};
  int Point::*coords[] = {&Point::x, &Point::y, &Point::z};
  for (int i = 0; i < 3; i++) printf("%d ", pt.*coords[i]);
  printf("\n");
  int Point::*none = nullptr;
  printf("%d %d %d\n", none == nullptr, coords[0] != coords[1], (bool)coords[2]);
  Point* pp = &pt;
  pp->*coords[1] = 20;
  printf("%d %d\n", pick(pt, &Point::y), pt.y);
  int (Point::*f)(int) = &Point::scale;
  int (Point::*g)() const = &Point::sum;
  printf("%d %d\n", (pt.*f)(2), (pp->*g)());
  int (Base::*w)() const = &Base::who;
  int (Base::*pl)() const = &Base::plain;
  Derived d; Base b;
  Base* bp = &d;
  printf("%d %d %d %d\n", (b.*w)(), (d.*w)(), (bp->*w)(), (bp->*pl)());
  int Derived::*e = &Derived::extra;
  printf("%d\n", d.*e);
  int (Base::*nf)() const = nullptr;
  printf("%d %d %d\n", nf == nullptr, w != pl, w == &Base::who);
  return 0;
}

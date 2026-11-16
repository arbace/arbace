// aggregates: brace initialization, nested aggregates, copies, passing and returning by value
#include <cstdio>
struct Point { int x, y; };
struct Rect { Point lo, hi; const char *name; };
struct Big { long v[6]; };
Point add(Point a, Point b) { return {a.x + b.x, a.y + b.y}; }
Big twice(Big b) { for (int i = 0; i < 6; i++) b.v[i] *= 2; return b; }
int area(const Rect &r) { return (r.hi.x - r.lo.x) * (r.hi.y - r.lo.y); }
int main() {
  Point p = {1, 2}, q{3, 4};
  Point s = add(p, q);
  printf("%d %d\n", s.x, s.y);
  Rect r = {{0, 0}, {4, 5}, "box"};
  printf("%s %d\n", r.name, area(r));
  Rect r2 = r; r2.hi.x = 10;
  printf("%d %d\n", area(r), area(r2));
  Big b = {{1, 2, 3}};
  Big c = twice(b);
  for (int i = 0; i < 6; i++) printf("%ld ", c.v[i]);
  printf("%ld\n", b.v[0]);
  Point pts[3] = {{1, 1}, {2, 2}};
  printf("%d %d\n", pts[1].y, pts[2].x);
  Point z{};
  printf("%d %d %d\n", z.x, z.y, (int)sizeof(Rect));
  return 0;
}

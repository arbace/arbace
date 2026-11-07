// default arguments of functions and methods (evaluated at each call), compound assignments on members through pointers
#include <cstdio>
int calls = 0;
int tick() { return ++calls; }
int f(int a, int b = 10, int c = tick()) { return a + b + c; }
struct Pen { int width; Pen(int w = 1) : width(w) {} int stroke(int len, int times = 2) const { return width * len * times; } };
int main() {
  printf("%d %d %d\n", f(1), f(1, 2), f(1, 2, 3));
  printf("calls %d\n", calls);
  Pen p, q(3);
  printf("%d %d %d\n", p.stroke(5), q.stroke(5), q.stroke(5, 1));
  Pen *pp = &q;
  pp->width += 2; pp->width *= 3;
  printf("%d\n", pp->width);
  return 0;
}

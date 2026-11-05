// the SysV classification in use: small classes returned and chained in expressions, a class with a float and an int,
// a class with a char array, nested classes, a class with bit-fields by value, methods returning small classes
#include <cstdio>
struct V2 { float x, y; V2 operator+(V2 o) const { return V2{x + o.x, y + o.y}; } V2 scaled(float k) const { return V2{x * k, y * k}; } };
struct Tagged { float f; int tag; };
struct Name { char s[6]; };
struct Outer { struct { short a; short b; } in; int c; };
struct Bits { unsigned a : 3, b : 5, c : 24; };
Tagged tag(float f, int t) { return Tagged{f, t}; }
Name name(const char* p) { Name n{}; for (int i = 0; i < 5 && p[i]; i++) n.s[i] = p[i]; return n; }
Outer outer(Outer o) { o.in.a += 1; o.c *= 2; return o; }
Bits flip(Bits b) { Bits r = b; r.a = 7 - b.a; return r; }
int main() {
  V2 a{1, 2}, b{3, 4};
  V2 c = (a + b).scaled(0.5f) + V2{10, 20};
  printf("%.2f %.2f\n", c.x, c.y);
  Tagged t = tag(2.5f, 9);
  printf("%.1f %d\n", t.f, t.tag);
  printf("%s %s\n", name("hello").s, name("abc").s);
  Outer o = outer(Outer{{1, 2}, 3});
  printf("%d %d %d\n", o.in.a, o.in.b, o.c);
  Bits x = flip(Bits{2, 17, 123456});
  printf("%u %u %u\n", x.a, x.b, x.c);
  return 0;
}

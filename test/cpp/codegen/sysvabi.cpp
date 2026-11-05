// the SysV x86-64 classification of classes passed and returned by value: two integer eightbytes, two SSE ones, mixed,
// a tail of four and of three bytes, floats packed in an eightbyte, an empty class, a MEMORY class, a non-trivial
// class by address, registers running out (the rest by address)
#include <cstdio>
struct Pair { int a, b; };
struct Wide { long a, b; };
struct Dbl2 { double x, y; };
struct Mix { double d; int i; };
struct Tri { float a, b, c; };
struct Bytes3 { char c[3]; };
struct Empty {};
struct Big { long v[4]; };
struct Counted { int n; Counted(int k) : n(k) {} Counted(const Counted& o) : n(o.n + 1) {} ~Counted() {} };
Pair swap(Pair p) { return Pair{p.b, p.a}; }
Wide add(Wide x, Wide y) { return Wide{x.a + y.a, x.b + y.b}; }
Dbl2 scale(Dbl2 v, double k) { return Dbl2{v.x * k, v.y * k}; }
Mix mix(Mix m) { m.d += m.i; m.i *= 2; return m; }
Tri rot(Tri t) { return Tri{t.b, t.c, t.a}; }
Bytes3 up(Bytes3 b) { for (char& c : b.c) c = c - 32; return b; }
int empty(Empty, int k, Empty) { return k + 1; }
Big twice(Big b) { for (long& x : b.v) x *= 2; return b; }
int counted(Counted c) { return c.n; }
long many(Wide a, Wide b, Wide c, Wide d, long e) { return a.a + b.b + c.a + d.b + e; }
int main() {
  Pair p = swap(Pair{1, 2});
  Wide w = add(Wide{1, 2}, Wide{30, 40});
  Dbl2 d = scale(Dbl2{1.5, 2.5}, 2);
  Mix m = mix(Mix{0.5, 3});
  Tri t = rot(Tri{1, 2, 3});
  Bytes3 b = up(Bytes3{{'a', 'b', 'c'}});
  Big g = twice(Big{{1, 2, 3, 4}});
  printf("%d %d | %ld %ld | %.1f %.1f | %.1f %d | %.0f %.0f %.0f\n", p.a, p.b, w.a, w.b, d.x, d.y, m.d, m.i, t.a, t.b, t.c);
  printf("%c%c%c %d %ld %ld %d %ld\n", b.c[0], b.c[1], b.c[2], empty(Empty{}, 4, Empty{}), g.v[0], g.v[3], counted(Counted(5)),
         many(Wide{1, 2}, Wide{3, 4}, Wide{5, 6}, Wide{7, 8}, 9));
  return 0;
}

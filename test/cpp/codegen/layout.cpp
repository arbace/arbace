// class layout by the Itanium ABI: a polymorphic class over a plain base (the base after the vptr), empty bases,
// tail padding of a non-POD base reused, alignment of members
#include <cstdio>
struct Plain { int a; char b; };
struct Poly : Plain { virtual ~Poly() {} int c = 3; };
struct Empty {};
struct UsesEmpty : Empty { int v = 9; };
struct NonPod { NonPod() {} int x = 1; char y = 2; };
struct Reuse : NonPod { char z = 3; };
struct Mixed { char c; double d; short s; long l; char t; };
int main() {
  Poly p; p.a = 1; p.b = 2;
  Plain *pl = &p;
  printf("%d %d %d %d\n", (int)sizeof(Poly), pl->a, pl->b, p.c);
  printf("%d %d %d\n", (int)((char *)pl - (char *)&p), (int)sizeof(UsesEmpty), UsesEmpty().v);
  Reuse r;
  printf("%d %d %d %d %d\n", (int)sizeof(NonPod), (int)sizeof(Reuse), r.x, r.y, r.z);
  Mixed m{1, 2.5, 3, 4, 5};
  printf("%d %d %d\n", (int)sizeof(Mixed), (int)((char *)&m.l - (char *)&m), (int)((char *)&m.t - (char *)&m));
  return 0;
}

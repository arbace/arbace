// G5: brace elision ([dcl.init.aggr]/16): an array member of an aggregate initialized from flat items, an array of
// arrays, an aggregate copied from one of its own type in braces ([dcl.init.list]/3.2: the parser's semantic form unwrapped)
#include <cstdio>
struct A3 { int e[3]; };
struct P { A3 a; P(int x, int y, int z) : a{x, y, z} {} P(const P& o) : a{o.a} {} };
struct Q { int a[2]; int b; };
int main() { P p(1, 2, 3); P q(p); Q r = {4, 5, 6}; int m[2][2] = {1, 2, 3, 4}; printf("%d %d %d %d %d %d\n", q.a.e[2], r.a[1], r.b, m[1][0], m[1][1], p.a.e[0]); return 0; }

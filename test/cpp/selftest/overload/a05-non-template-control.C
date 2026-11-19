// queue 423: the negative control, a05's case with its expectations moved (the two errors' lines unexpected, two valid lines
// expected): must fail
struct S { int a[2]; };
struct A { int x; };
struct C { int y; };
int f(const S&); int f(S&&);
int k(S&); int k(const S&);
int n(S&);
int m(const A&); int m(const C&);
void h() { S s; s = {}; s = S{}; s = {s}; s = {1, 2}; f({}); f({s}); k({}); k({s});   // { dg-error "control" }
  n({});
  m({});
  s = {};  // { dg-error "control" }
}

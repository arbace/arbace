// queue 423: braced lists for class references outside templates ([over.ics.list]/9 with [over.ics.rank]/3.2.3): the rvalue
// reference over the const lvalue one, a non-const lvalue reference binding no list's temporary, two unrelated aggregates
// still ambiguous ([over.ics.rank]/3: two user-defined conversions of different classes)
struct S { int a[2]; };
struct A { int x; };
struct C { int y; };
int f(const S&); int f(S&&);
int k(S&); int k(const S&);
int n(S&);
int m(const A&); int m(const C&);
void h() { S s; s = {}; s = S{}; s = {s}; s = {1, 2}; f({}); f({s}); k({}); k({s});
  n({});   // { dg-error "no matching" }
  m({});   // { dg-error "ambiguous" }
}

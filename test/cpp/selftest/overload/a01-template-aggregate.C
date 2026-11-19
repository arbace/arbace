// queue 423: T{} of a dependent type is T list-initialized ([expr.type.conv]/2), a prvalue; its assignment takes the implicit
// move assignment over the copy assignment ([over.ics.rank]/3.2.3). 412's repro: "ambiguous call (2 viable)" at T{}
struct S { int a[2]; };
struct P { int x; double y; };
template<class T> void g(T& t) { t = T{}; }
template<class T> void g2(T& t) { T u = T{}; t = (T{}); t = T({}); t = u; }
void h() { S s; g(s); int i; g(i); P p; g(p); g2(p); g2(s); }

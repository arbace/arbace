// G5: three-way comparison: the built-in <=> (integers: strong_ordering, floating: partial_ordering), defaulted
// member and friend operator<=> (the common category deduced) with the == they declare, relational operators rewritten
// as (x <=> y) @ 0 ([over.match.oper]/3.4), std::is_eq through a conversion function, __unspec's consteval constructor
#include <compare>
#include <cstdio>
struct P { int x; double y; auto operator<=>(const P&) const = default; bool operator==(const P&) const = default; };
struct Q { long a; int b; friend auto operator<=>(Q, Q) = default; };
int main() {
  P a{1, 2.0}, b{1, 3.0};
  Q c{5, 1}, d{5, 2};
  printf("%d %d %d %d\n", a < b, a == b, a != b, b >= a);
  printf("%d %d %d\n", c < d, d > c, (c <=> c) == 0);
  printf("%d %d %d\n", (1 <=> 2) < 0, (2.5 <=> 1.0) > 0, std::is_eq(3 <=> 3));
  return 0;
}

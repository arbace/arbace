// operator overloading: members and non-members, compound assignment, subscripts, calls, comparisons, increments
#include <cstdio>
struct V2 {
  int x, y;
  V2 operator+(const V2 &o) const { return {x + o.x, y + o.y}; }
  V2 &operator+=(const V2 &o) { x += o.x; y += o.y; return *this; }
  V2 operator-() const { return {-x, -y}; }
  bool operator==(const V2 &o) const { return x == o.x && y == o.y; }
  int &operator[](int i) { return i == 0 ? x : y; }
  int operator()(int k) const { return x * k + y; }
  V2 &operator++() { ++x; ++y; return *this; }
};
V2 operator*(int k, const V2 &v) { return {k * v.x, k * v.y}; }
bool operator<(const V2 &a, const V2 &b) { return a.x < b.x || (a.x == b.x && a.y < b.y); }
struct Acc { long total = 0; Acc &operator<<(long v) { total += v; return *this; } };
int main() {
  V2 a{1, 2}, b{3, 4};
  V2 c = a + b;
  printf("%d %d\n", c.x, c.y);
  c += a; c += a;
  printf("%d %d\n", c.x, c.y);
  V2 d = -c;
  V2 e = 3 * a;
  printf("%d %d %d %d\n", d.x, d.y, e.x, e.y);
  printf("%d %d %d\n", a == b, a == V2{1, 2}, a < b);
  c[1] = 50;
  printf("%d %d %d\n", c[0], c[1], c(10));
  ++a;
  printf("%d %d\n", a.x, a.y);
  Acc acc; acc << 1 << 2 << 39;
  printf("%ld\n", acc.total);
  V2 f = a; f = b;
  printf("%d %d\n", f.x, f.y);
  return 0;
}

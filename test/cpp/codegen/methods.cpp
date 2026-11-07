// member functions, this, const methods, static members, out-of-line definitions
#include <cstdio>
class Counter {
  int count;
  static int instances;
public:
  Counter() : count(0) { ++instances; }
  void inc() { ++count; }
  void add(int n);
  int get() const { return count; }
  Counter &self() { return *this; }
  static int total() { return instances; }
};
int Counter::instances = 0;
void Counter::add(int n) { for (int i = 0; i < n; i++) this->inc(); }
struct Vec { double x, y; double dot(const Vec &o) const { return x * o.x + y * o.y; } Vec scaled(double k) const { return {x * k, y * k}; } };
int main() {
  Counter a, b;
  a.inc(); a.add(4); b.self().inc();
  printf("%d %d %d\n", a.get(), b.get(), Counter::total());
  Counter *pc = &b; pc->add(2);
  printf("%d\n", pc->get());
  Vec v{1, 2}, w{3, 4};
  printf("%.1f %.1f\n", v.dot(w), v.scaled(3).dot(w));
  return 0;
}

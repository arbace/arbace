// an explicit specialization of a class template with members of its own
#include <cstdio>
template <class T> struct Traits { static int id() { return 1; } int width() const { return (int)sizeof(T); } };
template <> struct Traits<char> { static int id() { return 2; } int width() const { return 100; } int extra() const { return 7; } };
template <> struct Traits<bool> { int width() const { return -1; } };
int main() {
  Traits<int> a; Traits<char> b; Traits<bool> c; Traits<double> d;
  printf("%d %d %d\n", Traits<int>::id(), Traits<char>::id(), Traits<long>::id());
  printf("%d %d %d %d %d\n", a.width(), b.width(), c.width(), d.width(), b.extra());
  return 0;
}

// a member function template of a class template, with explicit and deduced arguments
#include <cstdio>
template <class T> struct Box {
  T v;
  template <class U> U as() const { return (U)v; }
  template <class U> T plus(U u) const { return v + (T)u; }
};
int main() {
  Box<double> b = {2.75};
  printf("%d %g %g\n", b.as<int>(), b.plus(3), b.plus('\1'));
  Box<int> c = {40};
  printf("%c %d\n", c.as<char>(), c.plus(2.9));
  return 0;
}

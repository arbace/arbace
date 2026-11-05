// class templates as arguments of class templates: nested template-ids and their substitutions in names
#include <cstdio>
template <class A, class B> struct Pair { A a; B b; A first() const { return a; } };
template <class T> struct Box { T v; T get() const { return v; } };
template <class T> T first_of(const Pair<T, Box<T> >& p) { return p.a + p.b.get(); }
int main() {
  Pair<Box<int>, Box<char> > p;
  p.a.v = 1; p.b.v = 'x';
  Pair<int, Box<int> > q = {2, {3}};
  printf("%d %c %d\n", p.first().get(), p.b.get(), first_of(q));
  return 0;
}

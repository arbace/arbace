// template template parameters: a class template's argument used as a base and a member, deduced from a call, given
// explicitly; their mangling (the template's name, T_<args>)
#include <cstdio>
template <class T> struct Holder { T v; int size() const { return (int)sizeof(T); } };
template <class T> struct Twice { T a, b; int size() const { return (int)sizeof(T) * 2; } };
template <template <class> class TT, class E> struct Wrap : TT<E> { TT<char> other; int both() const { return this->size() + other.size(); } };
template <template <class> class TT> int measure(const TT<int>& x) { return x.size(); }
template <template <class> class TT> int make() { TT<long> y; return y.size(); }
int main() {
  Wrap<Holder, int> w; Wrap<Twice, double> t;
  Holder<int> h; Twice<int> d;
  printf("%d %d %d %d %d %d\n", w.both(), t.both(), measure(h), measure(d), make<Holder>(), make<Twice>());
  return 0;
}

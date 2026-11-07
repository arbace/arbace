// G5: a member class of a class template declared in it and defined outside it (template <class T> class Out<T>::In),
// its constructor defined outside both (Out<T>::In::In), the out-of-line nested class of a plain class (class L::F)
// whose members see the enclosing class's names
#include <cstdio>
template<class T> struct Out {
  class In;
  T v;
  int get();
};
template<class T> class Out<T>::In {
public:
  T w;
  In(Out<T>& o);
  int twice() { return w * 2; }
};
template<class T> Out<T>::In::In(Out<T>& o) : w(o.v) {}
template<class T> int Out<T>::get() { In i(*this); return i.twice(); }
namespace N {
  typedef int* cl;
  class L { public: typedef int unit; class F; };
  class L::F { public: static cl c; static unit u; static cl get() { return c; } };
  cl L::F::c = 0;
  L::unit L::F::u = 5;
}
int main() {
  Out<int> o; o.v = 21; printf("%d\n", o.get());
  Out<long> p; p.v = 4; printf("%d\n", p.get());
  printf("%d %d\n", N::L::F::get() == 0, N::L::F::u);
  return 0;
}

// a partial specialization for pointers with its own data members and member functions
#include <cstdio>
template <class T> struct Holder { T v; Holder(T x) : v(x) {} T get() const { return v; } };
template <class T> struct Holder<T*> { T* p; Holder(T* x) : p(x) {} T get() const { return *p; } void set(T x) { *p = x; } };
int main() {
  int x = 5;
  Holder<int> a(3);
  Holder<int*> b(&x);
  b.set(9);
  printf("%d %d %d\n", a.get(), b.get(), x);
  return 0;
}

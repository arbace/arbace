// alias templates, variable templates (folded where used as values, defined linkonce_odr where odr-used), if constexpr
// discarding the branch that would not compile for the type
#include <cstdio>
template <class T> using Ptr = T*;
template <class T, int N> using Arr = T[N];
template <class T> constexpr bool is_big = sizeof(T) > 4;
template <class T> constexpr int width = (int)sizeof(T) * 8;
struct Small { char c; int name() const { return 1; } };
template <class T> int describe(T t) {
  if constexpr (is_big<T>) return width<T>;
  else return t.name() + 100;
}
int main() {
  int x = 5;
  Ptr<int> p = &x;
  Arr<char, 3> a = {'a', 'b', 'c'};
  const int* w = &width<double>;
  printf("%d %c %d %d %d %d\n", *p, a[2], (int)is_big<char>, describe(1L), describe(Small{'z'}), *w);
  return 0;
}

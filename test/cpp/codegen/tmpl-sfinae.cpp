// substitution failure is not an error: a template whose signature names T's member type drops out for int
#include <cstdio>
struct S { typedef long type; };
template <class T> int which(typename T::type) { return 1; }
template <class T> int which(T) { return 2; }
int main() {
  printf("%d %d\n", which<S>(3), which<int>(3));
  return 0;
}

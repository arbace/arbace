// partial specializations chosen by deduction: a pointer, a pair of equal types, the more specialized of two
#include <cstdio>
template <class T, class U> struct K { static int v() { return 0; } };
template <class T> struct K<T, T> { static int v() { return 1; } };
template <class T, class U> struct K<T*, U> { static int v() { return 2; } };
template <class T> struct K<T*, T*> { static int v() { return 3; } };
template <class T> struct K<T, int> { static int v() { return 4; } };
int main() {
  printf("%d %d %d %d %d\n", K<int, char>::v(), K<char, char>::v(), K<int*, char>::v(), K<int*, int*>::v(), K<char, int>::v());
  return 0;
}

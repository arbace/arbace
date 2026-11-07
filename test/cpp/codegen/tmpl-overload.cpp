// overload resolution with templates: a non-template preferred on a tie, the more specialized template chosen
#include <cstdio>
template <class T> int f(T) { return 1; }
template <class T> int f(T*) { return 2; }
template <class T> int f(const T*) { return 3; }
int f(int) { return 4; }
template <class T> int g(T, T) { return 5; }
template <class T, class U> int g(T, U) { return 6; }
int main() {
  int i = 0; const int ci = 0; double d = 0;
  printf("%d %d %d %d %d\n", f(i), f(d), f(&i), f(&ci), f('c'));
  printf("%d %d\n", g(1, 2), g(1, 2.0));
  return 0;
}

// forwarding references and reference collapsing: T deduced as an lvalue reference, a cast to T&&
#include <cstdio>
template <class T> T&& fwd(T& t) { return static_cast<T&&>(t); }
template <class T> int kind(T&) { return 1; }
template <class T> int kind(T&&) { return 2; }
template <class T> void bump(T&& t) { t += 1; }
int main() {
  int x = 1;
  bump(x);
  bump(x);
  int y = fwd<int>(x);
  printf("%d %d %d %d\n", x, y, kind(x), kind(3));
  return 0;
}

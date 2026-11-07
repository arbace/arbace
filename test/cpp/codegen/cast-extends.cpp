// lifetime extension through a cast to a glvalue of the operand's object ([class.temporary]/6, CWG1376; cpp-90): static_cast to
// an rvalue reference and const_cast keep the temporary alive to the reference's end; the destructor runs after the last use
#include <cstdio>
struct T { int v; T(int v) : v(v) { printf("+%d\n", v); } ~T() { printf("-%d\n", v); } };
int main() {
  T&& r = static_cast<T&&>(T(1));
  const T& q = const_cast<const T&>(static_cast<const T&>(T(2)));
  printf("use %d %d\n", r.v, q.v);
}

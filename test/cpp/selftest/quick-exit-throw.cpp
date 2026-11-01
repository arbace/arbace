// CP160 (b): a function registered with at_quick_exit that throws, and one registered first that must not run (the first
// terminates): the terminate message and status 134 as g++'s build
#include <cstdio>
#include <cstdlib>

static void first() { std::fputs("first\n", stderr); }
static void second() {
  std::fputs("second\n", stderr);
  throw 7;
}

int main() {
  std::at_quick_exit(first);
  std::at_quick_exit(second);
  std::fputs("main\n", stderr);
  std::quick_exit(0);
}

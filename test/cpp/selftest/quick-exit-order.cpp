// Report 066's R1: a handler registered by an at_quick_exit handler runs next (C11 7.22.4.7), before the older ones: q4 q2 q3 q1
#include <cstdio>
#include <cstdlib>

static void q1() { std::fputs("q1\n", stderr); }
static void q3() { std::fputs("q3\n", stderr); }
static void q2() {
  std::fputs("q2\n", stderr);
  std::at_quick_exit(q3);
}
static void q4() { std::fputs("q4\n", stderr); }

int main() {
  std::at_quick_exit(q1);
  std::at_quick_exit(q2);
  std::at_quick_exit(q4);
  std::quick_exit(0);
}

// Queue 107's F1: a quick_exit called by an at_quick_exit handler still runs the remaining handlers (musl's __funcs_on_quick_exit
// re-reads its list) and exits with the inner status: q3, q2, q1, status 5; the lowered program does the same
#include <cstdio>
#include <cstdlib>

static void q1() { std::fputs("q1\n", stderr); }
static void q2() {
  std::fputs("q2\n", stderr);
  std::quick_exit(5);
}
static void q3() { std::fputs("q3\n", stderr); }

int main() {
  std::at_quick_exit(q1);
  std::at_quick_exit(q2);
  std::at_quick_exit(q3);
  std::quick_exit(0);
}

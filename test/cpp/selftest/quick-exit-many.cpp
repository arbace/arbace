// Report 066's R2: musl's at_quick_exit keeps 32 handlers and refuses the rest (-1); the lowered program does the same
#include <cstdio>
#include <cstdlib>

static int ran;
static int failed;
static void h() { ++ran; }
static void last() { std::fprintf(stderr, "failed %d ran %d\n", failed, ran + 1); }

int main() {
  if (std::at_quick_exit(last)) ++failed;
  for (int i = 0; i < 39; ++i)
    if (std::at_quick_exit(h)) ++failed;
  std::quick_exit(0);
}

// exceptions: an exception that leaves a noexcept function calls std::terminate (status 134), after the frames below
// ran normally
#include <cstdio>
void inner(int x) { if (x > 1) throw x; }
void guarded(int x) noexcept { printf("guarded %d\n", x); fflush(stdout); inner(x); }
int main() {
  guarded(0);
  guarded(1);
  try { guarded(2); } catch (int) { printf("never\n"); }
  return 0;
}

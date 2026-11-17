// the status of main: computed from the work done, with a destructor run before the program ends
#include <cstdio>
struct Guard { int *p; ~Guard() { printf("guard %d\n", *p); } };
int collatz(long n) { int steps = 0; while (n != 1) { n = n % 2 ? 3 * n + 1 : n / 2; ++steps; } return steps; }
int main() {
  int best = 0, arg = 0;
  Guard g{&best};
  for (int i = 1; i < 1000; i++) { int s = collatz(i); if (s > best) { best = s; arg = i; } }
  printf("%d %d\n", arg, best);
  return best % 256;
}

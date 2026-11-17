// exceptions: an exception no handler catches terminates the program (status 134) after the message
#include <cstdio>
struct Bad { int v; };
void deep(int n) { if (n == 0) throw Bad{42}; deep(n - 1); }
int main() {
  printf("start\n");
  fflush(stdout);
  try { deep(2); } catch (int) { printf("wrong handler\n"); }
  printf("not reached\n");
  return 0;
}

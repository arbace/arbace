// G4: <cstdio> parsed in full (its bodies, its using-declarations into std): printf by its global and its std:: name,
// puts, snprintf into a buffer
#include <cstdio>
int main() {
  std::printf("%d %s\n", 42, "std");
  printf("%5.2f|\n", 3.14159);
  char buf[32];
  int n = std::snprintf(buf, sizeof buf, "<%d-%c>", 7, 'x');
  std::puts(buf);
  return n;
}

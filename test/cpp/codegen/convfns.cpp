// conversion functions: implicit and explicit ones, contextual conversion to bool, conversions in arguments, returns,
// arithmetic and casts, a base's conversion function inherited
#include <cstdio>
struct Meters { double v; operator double() const { return v; } };
struct Handle { int fd; explicit operator bool() const { return fd >= 0; } operator int() const { return fd; } };
struct Name { const char* s; operator const char*() const { return s; } };
struct Counter { int n; operator int() const { return n; } };
struct Sub : Counter { Sub(int k) { n = k * 10; } };
double half(double x) { return x / 2; }
int asInt(Counter c) { return c; }
int main() {
  Meters m{3.5};
  double d = m;
  printf("%.2f %.2f %.2f\n", d, half(m), m + 1.25);
  Handle good{4}, bad{-1};
  if (good) printf("good %d\n", (int)good);
  if (!bad) printf("bad\n");
  bool b = static_cast<bool>(good);
  printf("b %d sum %d\n", b, good + 10);
  Name nm{"conversion"};
  printf("%s\n", (const char*)nm);
  Sub s(7);
  int k = s;
  printf("%d %d %d\n", k, asInt(s), s * 2);
  return 0;
}

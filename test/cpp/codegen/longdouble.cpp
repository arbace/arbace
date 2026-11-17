// long double: x86 80-bit constants (literals to the last bit), arithmetic, conversions to and from double and
// integers, comparisons, long double through varargs and in a class
#include <cstdio>
struct Precise { long double v; long double twice() const { return v * 2; } };
long double third(long double x) { return x / 3; }
int main() {
  long double pi = 3.14159265358979323846264338327950288L;
  long double e = 2.71828182845904523536028747135266250L;
  long double tiny = 1e-4000L, huge = 1.5e4000L;
  printf("%.20Lf\n%.20Lf\n", pi, e);
  printf("%Le %Le\n", tiny, huge);
  double d = (double)pi;
  long double back = d;
  printf("%.20Lf %d\n", back, back == pi);
  long long n = (long long)(pi * 1e15L);
  printf("%lld %.5Lf\n", n, third(10.0L));
  Precise p{0.1L};
  printf("%.25Lf\n", p.twice());
  long double neg = -0.0L, one = 1;
  printf("%d %d %.3Lf\n", neg == 0, one > pi, -e);
  return 0;
}

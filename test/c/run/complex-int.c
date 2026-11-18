/* ci-run verdict: exit 0. GNU's complex integer types (CS105): their parts, arithmetic in the part type, conversions to and from the floating
   complex and the scalar types, equality, truth, negation and conjugation. Checks itself: each result's parts against
   those worked out by hand (division by gcc's method for an integer part type, tree-complex.cc's
   expand_complex_div_wide, Smith's: the ratio of the divisor's smaller part to its larger in integer division, so
   (10+5i)/(1+2i) is 2-5i, not 4-3i); a difference returns 1. */
#include <stdio.h>

typedef _Complex int ci;
typedef _Complex unsigned char cuc;
typedef _Complex long long cll;

/* the parts without __real__ and __imag__ (SPEC §14): a conversion to a real type takes the real part */
static int fails;
static void show(const char *n, cll z, long long re, long long im) {
  cll j = z * (cll)-1i; printf("%s %lld %lld\n", n, (long long)z, (long long)j);
  if ((long long)z != re || (long long)j != im) fails++; }

int main(void) {
  ci a = 3 + 4i, b = 1 - 2i;
  show("add", a + b, 4, 2);
  show("sub", a - b, 2, 6);
  show("mul", a * b, 11, -2);
  show("div", (ci)(10 + 5i) / (ci)(1 + 2i), 2, -5);
  show("div2", (ci)(10 + 5i) / (ci)(2 + 1i), 5, 2);
  show("div3", (cuc)(200 + 7i) / (cuc)(3 + 250i), 0, 0);
  show("neg", -a, -3, -4);
  show("conj", ~a, 3, -4);
  cuc u = 250 + 3i, v = 10 + 255i;
  show("uadd", (cuc)(u + v), 4, 2);
  show("umul", (cuc)(u * v), 199, 36);
  cll w = a;
  w *= 1000000;
  show("wide", w * w, -7000000000000, 24000000000000);
  _Complex double d = a;
  printf("dbl %g %g\n", (double)d, (double)(d * -1i));
  if ((double)d != 3 || (double)(d * -1i) != 4) fails++;
  ci back = (ci)(2.75 - 3.5i);
  show("back", back, 2, -3);
  int s = a;
  printf("scalar %d eq %d %d truth %d %d\n", s, a == 3 + 4i, a != b, !!(ci)0, !!(ci)1i);
  if (!(s == 3 && a == 3 + 4i && a != b && !(ci)0 && !!(ci)1i)) fails++;
  _Complex short arr[4] = { 1, 2i, 3 + 3i };
  show("arr", arr[2] + arr[1], 3, 5);
  return fails ? 1 : 0;
}

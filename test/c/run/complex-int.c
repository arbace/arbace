/* GNU's complex integer types (CS105): their parts, arithmetic in the part type, conversions to and from the floating
   complex and the scalar types, equality, truth, negation and conjugation. */
#include <stdio.h>

typedef _Complex int ci;
typedef _Complex unsigned char cuc;
typedef _Complex long long cll;

/* the parts without __real__ and __imag__ (SPEC §14): a conversion to a real type takes the real part */
static void show(const char *n, cll z) { cll j = z * (cll)-1i; printf("%s %lld %lld\n", n, (long long)z, (long long)j); }

int main(void) {
  ci a = 3 + 4i, b = 1 - 2i;
  show("add", a + b);
  show("sub", a - b);
  show("mul", a * b);
  show("div", (ci)(10 + 5i) / (ci)(1 + 2i));
  show("div2", (ci)(10 + 5i) / (ci)(2 + 1i));
  show("div3", (cuc)(200 + 7i) / (cuc)(3 + 250i));
  show("neg", -a);
  show("conj", ~a);
  cuc u = 250 + 3i, v = 10 + 255i;
  show("uadd", (cuc)(u + v));
  show("umul", (cuc)(u * v));
  cll w = a;
  w *= 1000000;
  show("wide", w * w);
  _Complex double d = a;
  printf("dbl %g %g\n", (double)d, (double)(d * -1i));
  ci back = (ci)(2.75 - 3.5i);
  show("back", back);
  int s = a;
  printf("scalar %d eq %d %d truth %d %d\n", s, a == 3 + 4i, a != b, !!(ci)0, !!(ci)1i);
  _Complex short arr[4] = { 1, 2i, 3 + 3i };
  show("arr", arr[2] + arr[1]);
  return 0;
}

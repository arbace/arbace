/* ci-run verdict: exit 0. gcc's __builtin_assoc_barrier (gcc 12): its operand's value, of its operand's type, which reassociation does not
   cross; narrow, wide and floating operands, and its operand's side effects once. Queue 126
   (gcc.dg/torture/bitint-74.c). Checks itself: a value other than the unreassociated
   sums' (1e16 + 1 rounds to 1e16) returns 1. */
#include <stdio.h>
_BitInt(255) b, c, d;
_BitInt(255) foo(void) { return __builtin_assoc_barrier(b + c) + d; }
int main(void) {
  b = 3162082328713384445049140446737468449630746270013462291267283007210433157591wb;
  c = 12998515555477887328635550261966833804427562203752161174274777867442907371807wb;
  d = 5016523343681809792116154509287659112784399275423992541459788346980443294044wb;
  _BitInt(255) r = foo();
  int eq = r == 21177121227873081565800845217991961366842707749189616007001849221633783823442wb;
  unsigned long long hi = (unsigned long long)(r >> 192);
  printf("%d %llx\n", eq, hi);
  volatile double x = 1e16, y = -1e16, z = 1.0; int i = 0;
  double s1 = __builtin_assoc_barrier(x + y) + z, s2 = __builtin_assoc_barrier(x + z) + y;
  printf("%g %g\n", s1, s2);
  int j = __builtin_assoc_barrier(i++);
  printf("%d %d %d\n", j, i, (int)sizeof(__builtin_assoc_barrier((char)1)));
  int g = _Generic(__builtin_assoc_barrier((unsigned short)1), unsigned short: 1, default: 0);
  printf("%d\n", g);
  return eq && hi == 0x2ed1d36b1f6fd0b6ULL && s1 == 1.0 && s2 == 0.0 && j == 0 && i == 1
         && sizeof(__builtin_assoc_barrier((char)1)) == 1 && g == 1 ? 0 : 1;
}

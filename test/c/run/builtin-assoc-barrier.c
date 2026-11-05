/* gcc's __builtin_assoc_barrier (gcc 12): its operand's value, of its operand's type, which reassociation does not
   cross; narrow, wide and floating operands, and its operand's side effects once. Queue 126
   (gcc.dg/torture/bitint-74.c). */
#include <stdio.h>
_BitInt(255) b, c, d;
_BitInt(255) foo(void) { return __builtin_assoc_barrier(b + c) + d; }
int main(void) {
  b = 3162082328713384445049140446737468449630746270013462291267283007210433157591wb;
  c = 12998515555477887328635550261966833804427562203752161174274777867442907371807wb;
  d = 5016523343681809792116154509287659112784399275423992541459788346980443294044wb;
  _BitInt(255) r = foo();
  printf("%d %llx\n", r == 21177121227873081565800845217991961366842707749189616007001849221633783823442wb, (unsigned long long)(r >> 192));
  volatile double x = 1e16, y = -1e16, z = 1.0; int i = 0;
  printf("%g %g\n", __builtin_assoc_barrier(x + y) + z, __builtin_assoc_barrier(x + z) + y);
  int j = __builtin_assoc_barrier(i++);
  printf("%d %d %d\n", j, i, (int)sizeof(__builtin_assoc_barrier((char)1)));
  printf("%d\n", _Generic(__builtin_assoc_barrier((unsigned short)1), unsigned short: 1, default: 0));
  return 0;
}

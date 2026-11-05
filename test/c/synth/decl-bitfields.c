// synth-c23/decl-bitfields.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// bit-fields: unsigned wrap, signed range, bool bit-fields, unnamed and zero-width
// ISO/IEC 9899:2024 6.7.3.2; Annex A: member-declarator (declarator_opt : constant-expression)
#include <stdio.h>

struct bf { unsigned u3 : 3; signed s3 : 3; bool flag : 1; unsigned : 0; unsigned rest : 5; unsigned : 2; };

int main(void) {
  struct bf b = {0};
  b.u3 = 9;
  b.s3 = 3;
  b.s3 = b.s3 + 1 - 8;
  b.flag = 5;
  b.rest = 31;
  b.rest++;
  printf("%s = %d\n", "b.u3", b.u3);
  printf("%s = %d\n", "b.s3", b.s3);
  printf("%s = %d\n", "b.flag", b.flag);
  printf("%s = %d\n", "b.rest", b.rest);
  printf("%s = %d\n", "_Generic(b.flag, bool: 1, default: 0)", _Generic(b.flag, bool: 1, default: 0));
  return 0;
}

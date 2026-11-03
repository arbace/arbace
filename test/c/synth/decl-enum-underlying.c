// synth-c23/decl-enum-underlying.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23 enumerations with a fixed underlying type and with values beyond int
// ISO/IEC 9899:2024 6.7.3.3; Annex A: enum-specifier, enum-type-specifier
// M.2: enumerations with a fixed underlying type; enumeration constants of types other than int
#include <stdio.h>

enum small : unsigned char { S0, S1 = 255 };
enum wide : long long { W0 = -1, W1 = 0x100000000 };
enum { BIG = 0x100000000, BIGGER };
enum fwd : short;
enum fwd : short { F0 = -3 };

int main(void) {
  enum small s = S1;
  s = s + 1;
  printf("%s = %zu\n", "sizeof(enum small)", sizeof(enum small));
  printf("%s = %d\n", "s", s);
  printf("%s = %d\n", "_Generic(S1, enum small: 1, default: 0)", _Generic(S1, enum small: 1, default: 0));
  printf("%s = %lld\n", "(long long)W1", (long long)W1);
  printf("%s = %lld\n", "(long long)W0", (long long)W0);
  printf("%s = %zu\n", "sizeof(enum wide)", sizeof(enum wide));
  printf("%s = %lld\n", "(long long)BIG", (long long)BIG);
  printf("%s = %lld\n", "(long long)BIGGER", (long long)BIGGER);
  printf("%s = %zu\n", "sizeof BIG", sizeof BIG);
  printf("%s = %d\n", "(int)F0", (int)F0);
  printf("%s = %zu\n", "sizeof(enum fwd)", sizeof(enum fwd));
  return 0;
}

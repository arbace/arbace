// synth-c23/lex-digit-separators.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// digit separators in every kind of constant
// ISO/IEC 9899:2024 6.4.4.1, 6.4.4.2; Annex A: digit, octal-digit, hexadecimal-digit, binary-digit, digit-sequence (with ')
// M.2: digit separators (')
#include <stdio.h>

int main(void) {
  printf("%s = %d\n", "1'000'000", 1'000'000);
  printf("%s = %d\n", "0x1'F", 0x1'F);
  printf("%s = %d\n", "0b1010'1010", 0b1010'1010);
  printf("%s = %d\n", "07'7", 07'7);
  printf("%s = %d\n", "1'2'3'4", 1'2'3'4);
  printf("%s = %lld\n", "1'000'000'000'000LL", 1'000'000'000'000LL);
  printf("%s = %.4f\n", "1'000.250'0", 1'000.250'0);
  printf("%s = %g\n", "1'0e1'0", 1'0e1'0);
  printf("%s = %g\n", "0x1'0p0", 0x1'0p0);
  return 0;
}

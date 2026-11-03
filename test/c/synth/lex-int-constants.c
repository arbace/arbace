// synth-c23/lex-int-constants.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// integer constants: decimal, octal, hexadecimal, binary, every suffix
// ISO/IEC 9899:2024 6.4.4.1; Annex A: integer-constant, decimal-constant, octal-constant, hexadecimal-constant, binary-constant, integer-suffix
// M.2: binary integer constants (0b, 0B)
#include <stdio.h>

int main(void) {
  printf("%s = %d\n", "42", 42);
  printf("%s = %d\n", "017", 017);
  printf("%s = %d\n", "0x1F", 0x1F);
  printf("%s = %d\n", "0X1f", 0X1f);
  printf("%s = %d\n", "0b1011", 0b1011);
  printf("%s = %d\n", "0B111", 0B111);
  printf("%s = %d\n", "0", 0);
  printf("%s = %u\n", "4294967295u", 4294967295u);
  printf("%s = %ld\n", "100l", 100l);
  printf("%s = %ld\n", "100L", 100L);
  printf("%s = %lld\n", "9223372036854775807LL", 9223372036854775807LL);
  printf("%s = %llu\n", "18446744073709551615ULL", 18446744073709551615ULL);
  printf("%s = %lu\n", "7ul", 7ul);
  printf("%s = %llu\n", "8uLL", 8uLL);
  printf("%s = %lld\n", "0x7fffffffffffffffll", 0x7fffffffffffffffll);
  printf("%s = %d\n", "_Generic(0x80000000, unsigned int: 1, default: 0)", _Generic(0x80000000, unsigned int: 1, default: 0));
  printf("%s = %d\n", "_Generic(2147483648, long: 1, long long: 2, default: 0)", _Generic(2147483648, long: 1, long long: 2, default: 0));
  printf("%s = %d\n", "_Generic(017u, unsigned int: 1, default: 0)", _Generic(017u, unsigned int: 1, default: 0));
  printf("%s = %d\n", "_Generic(0b1ull, unsigned long long: 1, default: 0)", _Generic(0b1ull, unsigned long long: 1, default: 0));
  return 0;
}

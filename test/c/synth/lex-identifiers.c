// synth-c23/lex-identifiers.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// identifiers: underscores, digits, universal character names and UTF-8 letters
// ISO/IEC 9899:2024 6.4.2.1, 6.4.3; Annex A: identifier, identifier-start, identifier-continue, universal-character-name
// M.2: identifiers by Unicode's UAX #31 (XID_Start, XID_Continue)
#include <stdio.h>

static int _x1 = 1, __y = 2, a_b_c = 3, Z9 = 4;
static int \u00e9t\u00e9 = 5;
static int café = 6;

int main(void) {
  printf("%s = %d\n", "_x1 + __y + a_b_c + Z9", _x1 + __y + a_b_c + Z9);
  printf("%s = %d\n", "été", été);
  printf("%s = %d\n", "caf\\u00e9", caf\u00e9);
  return 0;
}

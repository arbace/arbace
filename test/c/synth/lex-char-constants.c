// synth-c23/lex-char-constants.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// character constants: plain, escapes, octal, hexadecimal, L, u, U and u8
// ISO/IEC 9899:2024 6.4.4.5; Annex A: character-constant, encoding-prefix, c-char, escape-sequence, simple-escape-sequence, octal-escape-sequence, hexadecimal-escape-sequence, universal-character-name
// M.2: u8 character constants (of type unsigned char)
#include <stdio.h>

int main(void) {
  printf("%s = %d\n", "'a'", 'a');
  printf("%s = %d\n", "'\\n'", '\n');
  printf("%s = %d\n", "'\\t'", '\t');
  printf("%s = %d\n", "'\\0'", '\0');
  printf("%s = %d\n", "'\\101'", '\101');
  printf("%s = %d\n", "'\\x41'", '\x41');
  printf("%s = %d\n", "'\\''", '\'');
  printf("%s = %d\n", "'\\\"'", '\"');
  printf("%s = %d\n", "'\\\\'", '\\');
  printf("%s = %d\n", "'\\?'", '\?');
  printf("%s = %d\n", "'\\a'", '\a');
  printf("%s = %d\n", "'\\b'", '\b');
  printf("%s = %d\n", "'\\f'", '\f');
  printf("%s = %d\n", "'\\r'", '\r');
  printf("%s = %d\n", "'\\v'", '\v');
  printf("%s = %d\n", "L'a'", L'a');
  printf("%s = %d\n", "u'a'", u'a');
  printf("%s = %d\n", "U'a'", U'a');
  printf("%s = %d\n", "u8'a'", u8'a');
  printf("%s = %d\n", "U'\\u00e9'", U'\u00e9');
  printf("%s = %d\n", "u'\\u20AC'", u'\u20AC');
  printf("%s = %d\n", "(int)U'\\U0001F600'", (int)U'\U0001F600');
  printf("%s = %d\n", "_Generic(u8'a', unsigned char: 1, default: 0)", _Generic(u8'a', unsigned char: 1, default: 0));
  printf("%s = %d\n", "_Generic(u'a', unsigned short: 1, default: 0)", _Generic(u'a', unsigned short: 1, default: 0));
  printf("%s = %d\n", "_Generic(U'a', unsigned int: 1, default: 0)", _Generic(U'a', unsigned int: 1, default: 0));
  printf("%s = %d\n", "_Generic('a', int: 1, default: 0)", _Generic('a', int: 1, default: 0));
  return 0;
}

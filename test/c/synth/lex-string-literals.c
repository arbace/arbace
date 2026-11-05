// synth-c23/lex-string-literals.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// string literals: concatenation, escapes, encoding prefixes, trigraphs kept
// ISO/IEC 9899:2024 6.4.5; Annex A: string-literal, s-char-sequence, encoding-prefix
// M.2: trigraphs removed; u8 string literals of type unsigned char[]
#include <stdio.h>

int main(void) {
  printf("%s = %s\n", "\"ab\" \"cd\"", "ab" "cd");
  printf("%s = %s\n", "\"tab\\tend\"", "tab\tend");
  printf("%s = %s\n", "\"\\x41\\102C\"", "\x41\102C");
  printf("%s = %s\n", "\"??=??/??(\"", "??=??/??(");
  printf("%s = %zu\n", "sizeof \"abc\"", sizeof "abc");
  printf("%s = %zu\n", "sizeof u8\"\\u00e9\"", sizeof u8"\u00e9");
  printf("%s = %zu\n", "sizeof L\"ab\" / sizeof L\"ab\"[0]", sizeof L"ab" / sizeof L"ab"[0]);
  printf("%s = %zu\n", "sizeof u\"ab\" / sizeof u\"ab\"[0]", sizeof u"ab" / sizeof u"ab"[0]);
  printf("%s = %zu\n", "sizeof U\"ab\" / sizeof U\"ab\"[0]", sizeof U"ab" / sizeof U"ab"[0]);
  printf("%s = %zu\n", "sizeof \"a\" \"b\" \"c\"", sizeof "a" "b" "c");
  printf("%s = %d\n", "u8\"\\u00e9\"[0]", u8"\u00e9"[0]);
  printf("%s = %d\n", "u8\"\\u00e9\"[1]", u8"\u00e9"[1]);
  printf("%s = %d\n", "(int)U\"x\\U0001F600\"[1]", (int)U"x\U0001F600"[1]);
  printf("%s = %d\n", "_Generic(u8\"a\"[0], unsigned char: 1, default: 0)", _Generic(u8"a"[0], unsigned char: 1, default: 0));
  printf("%s = %d\n", "_Generic(&\"a\"[0], char *: 1, default: 0)", _Generic(&"a"[0], char *: 1, default: 0));
  return 0;
}

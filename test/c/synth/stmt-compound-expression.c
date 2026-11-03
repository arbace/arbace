// synth-c23/stmt-compound-expression.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// compound statements, expression statements and null statements
// ISO/IEC 9899:2024 6.8.3, 6.8.4; Annex A: compound-statement, block-item-list, block-item, expression-statement
#include <stdio.h>

int main(void) {
  int a = 1;
  { int b = 2; a += b; { int c = 3; a += c; } }
  ;
  ;;
  a;
  {}
  int i = 0;
  while (i++ < 5) ;
  (void)(a + 1);
  printf("%s = %d\n", "a", a);
  printf("%s = %d\n", "i", i);
  return 0;
}

// synth-c23/lex-punctuators.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// punctuators, digraphs and comments
// ISO/IEC 9899:2024 6.4.6, 6.4.9; Annex A: punctuator (digraphs <: :> <% %> %: %:%:)
#include <stdio.h>

%:define CAT(a, b) a %:%: b
static int arr<:3:> = <%1, 2, 3%>; /* a block comment */ // a line comment
static int CAT(x, y) = 7;

int main(void) {
  printf("%s = %d\n", "arr<:1:> + arr[2]", arr<:1:> + arr[2]);
  printf("%s = %d\n", "xy", xy);
  printf("%s = %d\n", "1 /* in */ + /**/ 2", 1 /* in */ + /**/ 2);
  return 0;
}

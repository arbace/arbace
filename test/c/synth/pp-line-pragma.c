// synth-c23/pp-line-pragma.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// #line, #pragma, _Pragma, the null directive, #warning, __LINE__
// ISO/IEC 9899:2024 6.10.6, 6.10.7, 6.10.8, 6.10.10, 6.10.11; Annex A: control-line (#line, #pragma, #, #warning), non-directive
// M.2: #warning
#include <stdio.h>

#
#pragma STDC FP_CONTRACT OFF
#pragma STDC FENV_ACCESS OFF
#pragma unknown_pragma_ignored
_Pragma("STDC CX_LIMITED_RANGE OFF")
#warning "a warning, not an error"
#line 500
static int at500 = __LINE__;
#line 700 "renamed.c"
static const char *file = __FILE__;
static int at700 = __LINE__;
#define LINE_HERE __LINE__

int main(void) {
  printf("%s = %d\n", "at500", at500);
  printf("%s = %d\n", "at700", at700);
  printf("%s = %s\n", "file", file);
  printf("%s = %d\n", "LINE_HERE > 700", LINE_HERE > 700);
  return 0;
}

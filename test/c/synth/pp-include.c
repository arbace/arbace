// synth-c23/pp-include.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// source file inclusion: quoted and angled forms, a macro naming the header, __has_include, #pragma once
// ISO/IEC 9899:2024 6.10.3, 6.10.1; Annex A: control-line (#include), has-include-expression
// M.2: __has_include

#define HDR <stdio.h>
#include HDR
#include "synth.h"
#include "synth.h"
#if __has_include(<stdio.h>)
static int has_stdio = 1;
#else
static int has_stdio = 0;
#endif
#if __has_include("synth.h") && !__has_include("no-such-synth-header.h") && !__has_include(<no/such/header.h>)
static int has_local = 1;
#else
static int has_local = 0;
#endif
#if defined(__has_include)
static int defined_has = 1;
#endif

int main(void) {
  printf("%s = %d\n", "SYNTH_H", SYNTH_H);
  printf("%s = %d\n", "synth_twice(21)", synth_twice(21));
  printf("%s = %d\n", "has_stdio", has_stdio);
  printf("%s = %d\n", "has_local", has_local);
  printf("%s = %d\n", "defined_has", defined_has);
  return 0;
}

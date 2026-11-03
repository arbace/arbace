// synth-c23/pp-predefined.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// predefined macros: __STDC__, __STDC_VERSION__, __STDC_HOSTED__, __STDC_UTF_16__, __STDC_UTF_32__
// ISO/IEC 9899:2024 6.10.10; Annex A: (predefined macro names)
// M.2: __STDC_VERSION__ 202311L
#include <stdio.h>

#ifdef __STDC_NO_VLA__
#define defined_check() 0
#else
#define defined_check() 1
#endif

int main(void) {
  printf("%s = %d\n", "__STDC__", __STDC__);
  printf("%s = %ld\n", "__STDC_VERSION__", __STDC_VERSION__);
  printf("%s = %d\n", "__STDC_HOSTED__", __STDC_HOSTED__);
  printf("%s = %d\n", "__STDC_UTF_16__", __STDC_UTF_16__);
  printf("%s = %d\n", "__STDC_UTF_32__", __STDC_UTF_32__);
  printf("%s = %d\n", "defined_check()", defined_check());
  printf("%s = %d\n", "__LINE__", __LINE__);
  return 0;
}

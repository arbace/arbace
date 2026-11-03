// synth-c23/lib-string-c23.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23's string functions in musl: strdup, strndup, memccpy
// ISO/IEC 9899:2024 7.26.2.2, 7.26.2.6, 7.26.2.7; Annex A: (library)
// M.2: strdup, strndup and memccpy added to the library

#define _XOPEN_SOURCE 700
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(void) {
  char *d = strdup("duplicate");
  char *n = strndup("abcdef", 3);
  char buf[16] = {};
  char *stop = memccpy(buf, "key=value", '=', sizeof buf);
  printf("%s = %s\n", "d", d);
  printf("%s = %s\n", "n", n);
  printf("%s = %zu\n", "strlen(n)", strlen(n));
  printf("%s = %s\n", "buf", buf);
  printf("%s = %d\n", "(int)(stop - buf)", (int)(stop - buf));
  return 0;
}

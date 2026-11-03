// synth-c23/lib-musl-tables.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// library functions whose musl sources include a table inside a declaration: strerror, iswalpha, iswpunct, wcwidth, iconv
// ISO/IEC 9899:2024 7.26.6.3, 7.32.2.1, 7.32.2.1.10; Annex A: (library)

#define _XOPEN_SOURCE 700
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <wctype.h>
#include <wchar.h>
#include <iconv.h>
static int latin1_to_utf8(void) {
  iconv_t cd = iconv_open("UTF-8", "ISO-8859-1");
  if (cd == (iconv_t)-1) return -1;
  char in[] = "\xe9t\xe9", out[16] = {0};
  char *ip = in, *op = out;
  size_t il = 3, ol = sizeof out - 1;
  size_t r = iconv(cd, &ip, &il, &op, &ol);
  iconv_close(cd);
  return r == (size_t)-1 ? -2 : (int)(op - out);
}

int main(void) {
  printf("%s = %s\n", "strerror(ENOENT)", strerror(ENOENT));
  printf("%s = %s\n", "strerror(ERANGE)", strerror(ERANGE));
  printf("%s = %s\n", "strerror(EDOM)", strerror(EDOM));
  printf("%s = %d\n", "iswalpha(L'a') != 0", iswalpha(L'a') != 0);
  printf("%s = %d\n", "iswalpha(L'7') != 0", iswalpha(L'7') != 0);
  printf("%s = %d\n", "iswalpha(0xe9) != 0", iswalpha(0xe9) != 0);
  printf("%s = %d\n", "iswpunct(L'!') != 0", iswpunct(L'!') != 0);
  printf("%s = %d\n", "iswpunct(L'a') != 0", iswpunct(L'a') != 0);
  printf("%s = %d\n", "wcwidth(L'a')", wcwidth(L'a'));
  printf("%s = %d\n", "wcwidth(0x4e00)", wcwidth(0x4e00));
  printf("%s = %d\n", "wcwidth(0x0301)", wcwidth(0x0301));
  printf("%s = %d\n", "latin1_to_utf8()", latin1_to_utf8());
  return 0;
}

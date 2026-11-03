/* queue 178 (CX8's finding 4): musl's sources that include a table inside a declaration (src/errno/strerror.c's
   __strerror.h, ctype's alpha.h, punct.h and wide.h, locale's codepages.h) were not read by ci (:split-declaration), so
   strerror, iswalpha, iswpunct, wcwidth and iconv were undefined functions; arbace.c.musl reads them now.
   Self-checking: status 0 when ci agrees with gcc 15.2 on musl 1.2.6, 1..N the first check that failed. */
#define _XOPEN_SOURCE 700
#include <errno.h>
#include <string.h>
#include <wctype.h>
#include <wchar.h>
#include <iconv.h>
int main(void) {
  if (strcmp(strerror(ENOENT), "No such file or directory") != 0) return 1;
  if (strcmp(strerror(EINVAL), "Invalid argument") != 0) return 2;
  if (strcmp(strerror(0), "No error information") != 0) return 3;
  if (!iswalpha(L'a') || iswalpha(L'1') || !iswalpha(0xe9)) return 4;
  if (!iswpunct(L'!') || iswpunct(L'a')) return 5;
  if (wcwidth(L'a') != 1 || wcwidth(0x4e00) != 2 || wcwidth(0x0301) != 0) return 6;
  iconv_t cd = iconv_open("UTF-8", "ISO-8859-1");
  if (cd == (iconv_t)-1) return 7;
  char in[] = "caf\xe9", out[16] = {0};
  char *ip = in, *op = out;
  size_t il = 4, ol = sizeof out - 1;
  if (iconv(cd, &ip, &il, &op, &ol) == (size_t)-1) return 8;
  iconv_close(cd);
  if (strcmp(out, "caf\xc3\xa9") != 0) return 9;
  return 0;
}

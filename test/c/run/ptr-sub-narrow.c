/* ci-run verdict: exit 0. Pointer minus integers narrower than 64 bits (doc/C23-SURVEY.md, "As built: review 006's fixes", finding 2): each
   subtrahend converted to the pointer's offset type before it is negated, as in p -= k. Checks itself:
   a value other than C's returns 1. */
#include <stdio.h>

static int a[300];

int main(void) {
  for (int i = 0; i < 300; i++) a[i] = i;
  int *p = a + 290;
  unsigned char uc = 2;
  unsigned short us = 3;
  _Bool b = 1;
  unsigned u = 4;
  signed char sc = -128;
  short s = -5;
  unsigned long ul = 6;
  int *q = a + 10;
  printf("%d %d %d %d\n", *(p - uc), *(p - us), *(p - b), *(p - u));
  printf("%d %d %d\n", *(q - sc), *(q - s), *(p - ul));
  char *c = (char *)(a + 1);
  printf("%d\n", (int)((int *)(c - (unsigned char)4) - a));
  int *r = p; r -= uc; r -= b;
  printf("%d\n", *r);
  int ok = *(p - uc) == 288 && *(p - us) == 287 && *(p - b) == 289 && *(p - u) == 286
    && *(q - sc) == 138 && *(q - s) == 15 && *(p - ul) == 284
    && (int)((int *)(c - (unsigned char)4) - a) == 0 && *r == 287;
  return ok ? 0 : 1;
}

// synth-c23/decl-type-specifiers.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// the basic type specifiers in their combinations
// ISO/IEC 9899:2024 6.7.3; Annex A: type-specifier
#include <stdio.h>

int main(void) {
  void *vp = 0;
  char c = 'c';
  signed char sc = -1;
  unsigned char uc = 255;
  short s = -2;
  short int si = 3;
  unsigned short us = 4;
  int i = -5;
  signed si2 = -6;
  unsigned u = 7;
  unsigned int ui = 8;
  long l = -9;
  long int li = 10;
  unsigned long ul = 11;
  long long ll = -12;
  long long int lli = 13;
  unsigned long long ull = 14;
  float f = 1.5f;
  double d = 2.5;
  long double ld = 3.5;
  bool b = 1;
  _Bool b2 = 2;
  signed long long int slli = -15;
  long unsigned int lui = 16;
  printf("%s = %d\n", "vp == 0", vp == 0);
  printf("%s = %d\n", "c", c);
  printf("%s = %d\n", "sc", sc);
  printf("%s = %d\n", "uc", uc);
  printf("%s = %d\n", "s", s);
  printf("%s = %d\n", "si", si);
  printf("%s = %d\n", "us", us);
  printf("%s = %d\n", "i", i);
  printf("%s = %d\n", "si2", si2);
  printf("%s = %u\n", "u", u);
  printf("%s = %u\n", "ui", ui);
  printf("%s = %ld\n", "l", l);
  printf("%s = %ld\n", "li", li);
  printf("%s = %lu\n", "ul", ul);
  printf("%s = %lld\n", "ll", ll);
  printf("%s = %lld\n", "lli", lli);
  printf("%s = %llu\n", "ull", ull);
  printf("%s = %g\n", "f", f);
  printf("%s = %g\n", "d", d);
  printf("%s = %g\n", "(double)ld", (double)ld);
  printf("%s = %d\n", "b", b);
  printf("%s = %d\n", "b2", b2);
  printf("%s = %lld\n", "slli", slli);
  printf("%s = %lu\n", "lui", lui);
  return 0;
}

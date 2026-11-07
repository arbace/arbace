// synth-c23/decl-storage-classes.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// storage-class specifiers: typedef, extern, static, register, auto, thread_local
// ISO/IEC 9899:2024 6.7.2; Annex A: storage-class-specifier
#include <stdio.h>

typedef int myint;
extern int ext;
int ext = 11;
static int file_static = 12;
static _Thread_local int tl1 = 13;
static thread_local int tl2 = 14;
static int bump(void) { static int n; return ++n; }

int main(void) {
  register int r = 15;
  auto int a = 16;
  myint m = 17;
  bump();
  bump();
  extern int ext;
  printf("%s = %d\n", "ext", ext);
  printf("%s = %d\n", "file_static", file_static);
  printf("%s = %d\n", "tl1", tl1);
  printf("%s = %d\n", "tl2", tl2);
  printf("%s = %d\n", "r", r);
  printf("%s = %d\n", "a", a);
  printf("%s = %d\n", "m", m);
  printf("%s = %d\n", "bump()", bump());
  return 0;
}

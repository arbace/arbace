// synth-c23/decl-alignment.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// alignment specifiers alignas and _Alignas, alignof
// ISO/IEC 9899:2024 6.7.6, 6.5.4.4; Annex A: alignment-specifier
#include <stdio.h>
#include <stdint.h>

static alignas(32) int a32;
static _Alignas(16) char c16[4];
struct al { char c; alignas(8) int i; };
static alignas(double) int ad;

int main(void) {
  #define offsetof_i() ((size_t)&((struct al *)0)->i % 8)
  printf("%s = %d\n", "(int)((uintptr_t)&a32 % 32)", (int)((uintptr_t)&a32 % 32));
  printf("%s = %d\n", "(int)((uintptr_t)c16 % 16)", (int)((uintptr_t)c16 % 16));
  printf("%s = %d\n", "alignof(struct al) >= 8", alignof(struct al) >= 8);
  printf("%s = %zu\n", "offsetof_i()", offsetof_i());
  printf("%s = %d\n", "(int)((uintptr_t)&ad % alignof(double))", (int)((uintptr_t)&ad % alignof(double)));
  return 0;
}

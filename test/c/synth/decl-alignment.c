// synth-c23/decl-alignment.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// alignment specifiers alignas and _Alignas, alignof
// ISO/IEC 9899:2024 6.7.6, 6.5.4.4; Annex A: alignment-specifier
#include <stdio.h>
#include <stdint.h>

static alignas(32) int a32;
static _Alignas(16) char c16[4];
struct al { char c; alignas(8) int i; };
static alignas(double) int ad;
static int deep(int n) { char c = 1; alignas(64) int a[3] = {n, n, n}; char d = 2; alignas(32) double x = n; if ((uintptr_t)a % 64 || (uintptr_t)&x % 32) return -1000; return c + d + a[2] + (n ? deep(n - 1) : 0); }

int main(void) {
  #define offsetof_i() ((size_t)&((struct al *)0)->i % 8)
  char pad = 0;
  alignas(128) char l128[3] = {pad};
  static alignas(256) short s256;
  printf("%s = %d\n", "(int)((uintptr_t)&a32 % 32)", (int)((uintptr_t)&a32 % 32));
  printf("%s = %d\n", "(int)((uintptr_t)c16 % 16)", (int)((uintptr_t)c16 % 16));
  printf("%s = %d\n", "alignof(struct al) >= 8", alignof(struct al) >= 8);
  printf("%s = %zu\n", "offsetof_i()", offsetof_i());
  printf("%s = %d\n", "(int)((uintptr_t)&ad % alignof(double))", (int)((uintptr_t)&ad % alignof(double)));
  printf("%s = %d\n", "deep(4)", deep(4));
  printf("%s = %d\n", "(int)((uintptr_t)l128 % 128)", (int)((uintptr_t)l128 % 128));
  printf("%s = %d\n", "(int)((uintptr_t)&s256 % 256)", (int)((uintptr_t)&s256 % 256));
  return 0;
}

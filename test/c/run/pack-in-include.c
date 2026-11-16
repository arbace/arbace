/* ci-run verdict: exit 0. #pragma pack in function bodies of an included file (the Mac VM's report 261, finding 5: ci laid
   out _ctypes_test_generated.c.h's Packed1-3 at 16/8 where gcc gives 9/1, 10/2, 12/4), nested in blocks, a pop just before
   a block's closing brace, and pack at the header's file scope. gcc's pragmas are tokens (extend.texi, "Structure-Layout
   Pragmas": #pragma pack(push, n) and pop set the maximum member alignment for the definitions after them), whichever file
   brings them. The program checks itself against the layouts gcc 15.2 gives on x86-64 (the report's measurement) and
   aborts on a difference. Arbace's own, EPL-1.0. */
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <stdint.h>
static int fails;
#define EQ(x, v) do { if ((size_t)(x) != (size_t)(v)) { printf("FAIL %s: %zu, want %zu\n", #x, (size_t)(x), (size_t)(v)); fails++; } } while (0)
#define GCC_ATTR(x) __attribute__((x))
#include "pack-in-include.h"
int main(void) {
  struct M { char c; int i; };
  EQ(sizeof(struct H1), 5); EQ(sizeof(struct H2), 8); EQ(sizeof(struct M), 8);
  packed_sizes("1"); packed_sizes("2"); packed_sizes("4");
  printf("H1 %zu H2 %zu M %zu\n", sizeof(struct H1), sizeof(struct H2), sizeof(struct M));
  printf("pack-in-include: %d failing\n", fails);
  fflush(stdout);
  if (fails) abort();
  return 0;
}

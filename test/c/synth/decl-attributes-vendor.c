// synth-c23/decl-attributes-vendor.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// vendor attributes with prefixes and balanced token arguments, unknown ones ignored
// ISO/IEC 9899:2024 6.7.13.2; Annex A: attribute-prefixed-token, attribute-prefix, balanced-token-sequence, balanced-token
#include <stdio.h>

[[gnu::unused]] static int g1 = 1;
[[gnu::always_inline]] static inline int g2(void) { return 2; }
[[__gnu__::__noinline__]] static int g3(void) { return 3; }
[[vendor::odd(1, [2], {3, (4)})]] static int g4 = 4;
[[gnu::aligned(16)]] static int g5 = 5;
struct [[gnu::packed]] pk { unsigned char c; unsigned short s; };

int main(void) {
  printf("%s = %d\n", "g1", g1);
  printf("%s = %d\n", "g2()", g2());
  printf("%s = %d\n", "g3()", g3());
  printf("%s = %d\n", "g4", g4);
  printf("%s = %d\n", "g5", g5);
  printf("%s = %zu\n", "sizeof(struct pk)", sizeof(struct pk));
  return 0;
}

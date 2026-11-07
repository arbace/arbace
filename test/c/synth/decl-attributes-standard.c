// synth-c23/decl-attributes-standard.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// the standard attributes on declarations: deprecated, nodiscard, maybe_unused, noreturn, unsequenced, reproducible
// ISO/IEC 9899:2024 6.7.13; Annex A: attribute-specifier-sequence, attribute-specifier, attribute-list, attribute, attribute-token, standard-attribute, attribute-argument-clause
// M.2: attributes: deprecated, fallthrough, maybe_unused, nodiscard, noreturn, unsequenced, reproducible
#include <stdio.h>
#include <stdlib.h>

[[deprecated]] static int old(void) { return 1; }
[[deprecated("use new")]] static int older(void) { return 2; }
[[nodiscard]] static int must(void) { return 3; }
[[nodiscard("check it")]] static int must2(void) { return 4; }
[[maybe_unused]] static int unused_fn(void) { return 5; }
[[noreturn]] static void quit(void) { exit(0); }
static int pure_sq(int x) [[unsequenced]] { return x * x; }
static int repro(int x) [[reproducible]] { return x + 1; }
[[__nodiscard__, __maybe_unused__]] static int both(void) { return 6; }
[[]] static int empty_attrs = 7;

int main(void) {
  [[maybe_unused]] int unused_local = 0;
  if (must() == 0) quit();
  printf("%s = %d\n", "old()", old());
  printf("%s = %d\n", "older()", older());
  printf("%s = %d\n", "must2()", must2());
  printf("%s = %d\n", "pure_sq(5)", pure_sq(5));
  printf("%s = %d\n", "repro(5)", repro(5));
  printf("%s = %d\n", "both()", both());
  printf("%s = %d\n", "empty_attrs", empty_attrs);
  return 0;
}

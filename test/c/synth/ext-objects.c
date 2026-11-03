// synth-c23/ext-objects.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// external object definitions: initializers with addresses, arrays of strings, pointer arithmetic
// ISO/IEC 9899:2024 6.9.3, 6.7.11; Annex A: external-declaration, declaration
#include <stdio.h>

static int data[5] = {10, 20, 30, 40, 50};
static int *mid = &data[2];
static int *end = data + 5;
static const char *names[] = {"zero", "one", "two"};
static const char *const greeting = "hi" " there";
static struct { const char *k; int v; } kv[] = {{"a", 1}, {"b", 2}};
static int (*fp)(const char *, ...) = printf;

int main(void) {
  printf("%s = %d\n", "*mid", *mid);
  printf("%s = %d\n", "(int)(end - mid)", (int)(end - mid));
  printf("%s = %s\n", "names[2]", names[2]);
  printf("%s = %s\n", "greeting", greeting);
  printf("%s = %s\n", "kv[1].k", kv[1].k);
  printf("%s = %d\n", "kv[1].v", kv[1].v);
  printf("%s = %d\n", "fp == printf", fp == printf);
  return 0;
}

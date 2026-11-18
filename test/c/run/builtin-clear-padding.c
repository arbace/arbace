/* ci-run verdict: exit 0. gcc's __builtin_clear_padding (gcc 11): the padding bits of the object a pointer points to set to zero, as the
   pointed-to type lays it out: the bytes between and after members, the bits of a bit-field unit no named bit-field
   holds (an unnamed bit-field's among them), a union's bits that are padding in every member, an array's elements'
   and nested records'. Types whose padding is the target's (long double, _BitInt, an unnamed bit-field's declared
   type: AAPCS64 aligns the structure by it, the x86-64 psABI does not) are left out. Queue 126
   (gcc.dg/c23-tag-composite-10.c). Checks itself: each object's bytes against the x86-64 psABI's layout of its type
   worked out by hand (00 for a padding byte or bit, the value's ff or ab elsewhere; a _Bool's byte is all value, as gcc
   clears it, and the builtin's operand is evaluated once); a difference
   returns 1. */
#include <stdio.h>
#include <string.h>
struct A { char c; int i; short s; };                       /* 3 bytes after c, 2 at the end */
struct B { int a : 1; int : 3; int b : 1; };                /* c23-tag-composite-10's, int : 3 for its long : 3 */
struct C { unsigned a : 3, : 2, b : 4; unsigned char d; };  /* bits 3-4 and 9-15 */
union U { char c; short s; struct { char x; int y; } st; }; /* the struct's padding, but byte 1 is s's */
struct D { struct A a[2]; union U u; char tail; };
struct E { char c; struct { int x; char y; }; double d; };  /* an anonymous member */
struct F { unsigned char a : 4; unsigned char : 0; unsigned char b : 2; };
static int fails;
static void held(const char *got, const char *want) { if (strcmp(got, want)) { fprintf(stderr, "FAIL %s, want %s\n", got, want); fails++; } }
#define SHOW(T, W, ...) do { T v = __VA_ARGS__; unsigned char b[sizeof v]; char h_[3 * sizeof v + 1]; h_[0] = 0; \
    memset(&v, 0xff, sizeof v); __builtin_clear_padding(&v); memcpy(b, &v, sizeof v); printf("%-8s", #T); \
    for (unsigned i = 0; i < sizeof v; i++) { printf(" %02x", b[i]); sprintf(h_ + 3 * i, " %02x", b[i]); } puts(""); \
    held(h_ + 1, W); } while (0)
int main(void) {
#define A_ "ff 00 00 00 ff ff ff ff ff ff 00 00"
#define U_ "ff ff 00 00 ff ff ff ff"
  SHOW(struct A, A_, {0}); SHOW(struct B, "11 00 00 00", {0}); SHOW(struct C, "e7 01 ff 00", {0}); SHOW(union U, U_, {0});
  SHOW(struct D, A_ " " A_ " " U_ " ff 00 00 00", {0});
  SHOW(struct E, "ff 00 00 00 ff ff ff ff ff 00 00 00 00 00 00 00 ff ff ff ff ff ff ff ff", {0}); SHOW(struct F, "0f 03", {0});
  SHOW(int, "ff ff ff ff", 0); SHOW(_Bool, "ff", 0); SHOW(char *, "ff ff ff ff ff ff ff ff", 0);
  { struct A arr[3]; memset(arr, 0xab, sizeof arr); __builtin_clear_padding(&arr);
    for (unsigned i = 0; i < sizeof arr; i++) { printf("%02x", ((unsigned char *)arr)[i]); } puts("");
    static const unsigned char e[12] = { 0xab, 0, 0, 0, 0xab, 0xab, 0xab, 0xab, 0xab, 0xab, 0, 0 };
    for (unsigned i = 0; i < sizeof arr; i++) if (((unsigned char *)arr)[i] != e[i % 12]) fails++; }
  { struct A x; memset(&x, 0xff, sizeof x); struct A *p = &x; __builtin_clear_padding(p++);
    printf("%d %02x\n", (int)(p - &x), ((unsigned char *)&x)[1]);
    if (p - &x != 1 || ((unsigned char *)&x)[1] != 0) fails++; }
  return fails ? 1 : 0;
}

/* gcc's __builtin_clear_padding (gcc 11): the padding bits of the object a pointer points to set to zero, as the
   pointed-to type lays it out: the bytes between and after members, the bits of a bit-field unit no named bit-field
   holds (an unnamed bit-field's among them), a union's bits that are padding in every member, an array's elements'
   and nested records'. Types whose padding is the target's (long double, _BitInt, an unnamed bit-field's declared
   type: AAPCS64 aligns the structure by it, the x86-64 psABI does not) are left out. Queue 126
   (gcc.dg/c23-tag-composite-10.c). */
#include <stdio.h>
#include <string.h>
struct A { char c; int i; short s; };                       /* 3 bytes after c, 2 at the end */
struct B { int a : 1; int : 3; int b : 1; };                /* c23-tag-composite-10's, int : 3 for its long : 3 */
struct C { unsigned a : 3, : 2, b : 4; unsigned char d; };  /* bits 3-4 and 9-15 */
union U { char c; short s; struct { char x; int y; } st; }; /* the struct's padding, but byte 1 is s's */
struct D { struct A a[2]; union U u; char tail; };
struct E { char c; struct { int x; char y; }; double d; };  /* an anonymous member */
struct F { unsigned char a : 4; unsigned char : 0; unsigned char b : 2; };
#define SHOW(T, ...) do { T v = __VA_ARGS__; unsigned char b[sizeof v]; memset(&v, 0xff, sizeof v); \
    __builtin_clear_padding(&v); memcpy(b, &v, sizeof v); printf("%-8s", #T); \
    for (unsigned i = 0; i < sizeof v; i++) { printf(" %02x", b[i]); } puts(""); } while (0)
int main(void) {
  SHOW(struct A, {0}); SHOW(struct B, {0}); SHOW(struct C, {0}); SHOW(union U, {0}); SHOW(struct D, {0});
  SHOW(struct E, {0}); SHOW(struct F, {0}); SHOW(int, 0); SHOW(_Bool, 0); SHOW(char *, 0);
  { struct A arr[3]; memset(arr, 0xab, sizeof arr); __builtin_clear_padding(&arr);
    for (unsigned i = 0; i < sizeof arr; i++) { printf("%02x", ((unsigned char *)arr)[i]); } puts(""); }
  { struct A x; memset(&x, 0xff, sizeof x); struct A *p = &x; __builtin_clear_padding(p++);
    printf("%d %02x\n", (int)(p - &x), ((unsigned char *)&x)[1]); }
  return 0;
}

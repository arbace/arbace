/* GNU's ms_struct attribute (the Mac VM's report 244, item 4): the Microsoft layout gcc documents (x86 type attributes,
   -mms-bitfields), each case's size, alignment, offsets and bit positions as gcc 15.2 gives them on x86-64 (measured by
   hand); the program checks itself (abort on a difference) and prints what it measured. */
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#define MS __attribute__((ms_struct))
static int fails;
#define EQ(x, v) do { if ((size_t)(x) != (size_t)(v)) { printf("FAIL %s: %zu, want %zu\n", #x, (size_t)(x), (size_t)(v)); fails++; } } while (0)
/* the byte holding a bit-field's lowest bit and that bit's position in it: the field set to all ones in a zeroed object */
#define BIT(T, f, byte, bit) do { T x; memset(&x, 0, sizeof x); x.f = -1; const unsigned char *b = (const unsigned char *)&x; \
    size_t i = 0; while (i < sizeof x && !b[i]) i++; int k = 0; while (i < sizeof x && !((b[i] >> k) & 1)) k++; \
    printf("%s.%s at %zu:%d\n", #T, #f, i, k); EQ(i, byte); EQ(k, bit); } while (0)
struct MS t1 { unsigned int bf_1 : 12; unsigned int : 0; unsigned int bf_2 : 12; };
struct MS t2 { char foo : 4; short : 0; char bar; };
struct MS t3 { char foo : 4; short : 0; double bar; };
struct MS t4 { char foo : 6; int : 0; };
struct MS t5 { char foo; int : 0; char bar; };
struct MS t6 { char a : 4; short b : 4; };
struct MS t7 { int a : 4; int b : 30; };
struct MS t8 { char a; int b : 4; char c; };
struct MS t9 { int a : 4; unsigned b : 4; long long c : 3; char d : 2; };
struct __attribute__((ms_struct, packed)) t10 { char a; int b : 4; char c; };
struct MS t11 { char a : 4; char : 0; char b; short c : 3; };
struct MS t12 { short a : 9; short b : 9; short c : 9; };
union MS u1 { char a : 4; int : 20; };
struct __attribute__((gcc_struct)) g1 { char a : 4; short b : 4; };
struct MS t13 { char a; long long : 0; char b; };
struct MS t14 { long long a : 3; int : 0; char b; };
#pragma pack(push, 1)
struct MS t15 { char a; int b : 4; char c; };
struct MS t16 { int a : 4; int : 0; char b; };
#pragma pack(pop)
#pragma pack(push, 2)
struct MS t17 { char a; long long b : 4; char c; };
#pragma pack(pop)
struct MS t18 { _Bool a : 1; _Bool b : 1; char c : 2; };
struct MS t19 { char a : 4; int b : 4 __attribute__((aligned(8))); char c; };
int main(void) {
  EQ(sizeof(struct t1), 8); EQ(_Alignof(struct t1), 4); BIT(struct t1, bf_1, 0, 0); BIT(struct t1, bf_2, 4, 0);
  EQ(sizeof(struct t2), 4); EQ(_Alignof(struct t2), 2); EQ(offsetof(struct t2, bar), 2);
  EQ(sizeof(struct t3), 16); EQ(_Alignof(struct t3), 8); EQ(offsetof(struct t3, bar), 8);
  EQ(sizeof(struct t4), 4); EQ(_Alignof(struct t4), 4);
  EQ(sizeof(struct t5), 2); EQ(_Alignof(struct t5), 1); EQ(offsetof(struct t5, bar), 1);
  EQ(sizeof(struct t6), 4); EQ(_Alignof(struct t6), 2); BIT(struct t6, a, 0, 0); BIT(struct t6, b, 2, 0);
  EQ(sizeof(struct t7), 8); EQ(_Alignof(struct t7), 4); BIT(struct t7, a, 0, 0); BIT(struct t7, b, 4, 0);
  EQ(sizeof(struct t8), 12); EQ(_Alignof(struct t8), 4); EQ(offsetof(struct t8, c), 8); BIT(struct t8, b, 4, 0);
  EQ(sizeof(struct t9), 24); EQ(_Alignof(struct t9), 8);
  BIT(struct t9, a, 0, 0); BIT(struct t9, b, 0, 4); BIT(struct t9, c, 8, 0); BIT(struct t9, d, 16, 0);
  EQ(sizeof(struct t10), 6); EQ(_Alignof(struct t10), 1); EQ(offsetof(struct t10, c), 5); BIT(struct t10, b, 1, 0);
  EQ(sizeof(struct t11), 4); EQ(_Alignof(struct t11), 2); EQ(offsetof(struct t11, b), 1); BIT(struct t11, a, 0, 0); BIT(struct t11, c, 2, 0);
  EQ(sizeof(struct t12), 6); EQ(_Alignof(struct t12), 2); BIT(struct t12, a, 0, 0); BIT(struct t12, b, 2, 0); BIT(struct t12, c, 4, 0);
  EQ(sizeof(union u1), 4); EQ(_Alignof(union u1), 4);
  EQ(sizeof(struct g1), 2); EQ(_Alignof(struct g1), 2); BIT(struct g1, b, 0, 4);
  EQ(sizeof(struct t13), 2); EQ(_Alignof(struct t13), 1); EQ(offsetof(struct t13, b), 1);
  EQ(sizeof(struct t14), 16); EQ(_Alignof(struct t14), 8); EQ(offsetof(struct t14, b), 8);
  EQ(sizeof(struct t15), 6); EQ(_Alignof(struct t15), 1); EQ(offsetof(struct t15, c), 5); BIT(struct t15, b, 1, 0);
  EQ(sizeof(struct t16), 5); EQ(_Alignof(struct t16), 1); EQ(offsetof(struct t16, b), 4);
  EQ(sizeof(struct t17), 12); EQ(_Alignof(struct t17), 2); EQ(offsetof(struct t17, c), 10); BIT(struct t17, b, 2, 0);
  EQ(sizeof(struct t18), 1); EQ(_Alignof(struct t18), 1); BIT(struct t18, a, 0, 0); BIT(struct t18, b, 0, 1); BIT(struct t18, c, 0, 2);
  EQ(sizeof(struct t19), 16); EQ(_Alignof(struct t19), 8); EQ(offsetof(struct t19, c), 12); BIT(struct t19, b, 8, 0);
  /* values stored and read back through the units */
  struct t9 v = { 5, 9, -2, 1 };
  printf("t9 %d %u %lld %d\n", v.a, v.b, (long long)v.c, v.d);
  if (v.a != 5 || v.b != 9 || v.c != -2 || v.d != 1) fails++;
  printf("ms-struct: %d failing\n", fails);
  if (fails) abort();
  return 0;
}

/* pack-in-include.c's header (Arbace's own, EPL-1.0): #pragma pack in function bodies of an included file, as CPython's
   Modules/_ctypes/_ctypes_test_generated.c.h has them, and at its file scope. */
#pragma pack(push, 1)
struct H1 { char c; int i; };
#pragma pack(pop)
struct H2 { char c; int i; };

static int packed_sizes(const char *which) {
  if (which[0] == '1') {
    {
#pragma pack(push, 1)
      struct GCC_ATTR(ms_struct) Packed1 { int8_t a; int64_t b; };
#pragma pack(pop)
      EQ(sizeof(struct Packed1), 9); EQ(_Alignof(struct Packed1), 1); EQ(offsetof(struct Packed1, b), 1);
      printf("Packed1 %zu %zu\n", sizeof(struct Packed1), _Alignof(struct Packed1));
    }
  } else if (which[0] == '2') {
#pragma pack(push, 2)
    struct Packed2 { int8_t a; int64_t b; };
#pragma pack(pop)
    EQ(sizeof(struct Packed2), 10); EQ(_Alignof(struct Packed2), 2);
    printf("Packed2 %zu %zu\n", sizeof(struct Packed2), _Alignof(struct Packed2));
  } else {
#pragma pack(push, 4)
    struct Packed4 { int8_t a; int64_t b; };
    {
      struct Inner4 { char c; double d; };
      EQ(sizeof(struct Inner4), 12);
#pragma pack(pop)
    }
    struct After { int8_t a; int64_t b; };
    EQ(sizeof(struct Packed4), 12); EQ(_Alignof(struct Packed4), 4); EQ(sizeof(struct After), 16);
    printf("Packed4 %zu %zu After %zu\n", sizeof(struct Packed4), _Alignof(struct Packed4), sizeof(struct After));
  }
  return 0;
}

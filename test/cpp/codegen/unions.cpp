// unions: members at offset 0, the size of the largest, aggregate initialization of the first member, anonymous
// unions in a class and in a block, a tagged union, unnamed structs as members
#include <cstdio>
union Num { int i; float f; unsigned char bytes[4]; };
struct Value {
  enum Kind { Int, Dbl } kind;
  union { int iv; double dv; };
  struct { short lo, hi; } pair;
  void print() const { if (kind == Int) printf("int %d\n", iv); else printf("dbl %.2f\n", dv); }
};
int main() {
  Num n = {0x41424344};
  printf("%d %d %d %d size %zu\n", n.bytes[0], n.bytes[1], n.bytes[2], n.bytes[3], sizeof(Num));
  n.f = 1.0f;
  printf("%x\n", n.i);
  Value a; a.kind = Value::Int; a.iv = 42; a.print();
  Value b; b.kind = Value::Dbl; b.dv = 2.5; b.print();
  b.pair.lo = 3; b.pair.hi = 4;
  printf("pair %d %d size %zu\n", b.pair.lo, b.pair.hi, sizeof(Value));
  union { int x; char c[4]; };
  x = 0x00636261;
  printf("%s\n", c);
  Num z{};
  printf("%d\n", z.i);
  return 0;
}

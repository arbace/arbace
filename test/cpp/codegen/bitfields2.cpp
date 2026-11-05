// bit-fields: the SysV layout (units of the declared type, zero-width and unnamed fields), signed and unsigned
// extraction, stores that keep the neighbours, compound assignment and increments, sizeof
#include <cstdio>
struct Flags { unsigned a : 1; unsigned b : 3; int c : 5; unsigned : 0; unsigned d : 7; bool e : 1; };
struct Mixed { char tag; int lo : 12; int hi : 12; short s : 4; long long big : 40; };
struct Packed3 { unsigned char x : 3, y : 3, z : 2; };
int main() {
  Flags f{};
  f.a = 1; f.b = 5; f.c = -7; f.d = 100; f.e = true;
  printf("%u %u %d %u %d size %zu\n", f.a, f.b, f.c, f.d, (int)f.e, sizeof(Flags));
  f.b += 4; f.c++; f.d--; f.a = 3;
  printf("%u %u %d %u %d\n", f.a, f.b, f.c, f.d, (int)f.e);
  Mixed m = {'q', -100, 2047, -3, -123456789012LL};
  printf("%c %d %d %d %lld size %zu\n", m.tag, m.lo, m.hi, m.s, m.big, sizeof(Mixed));
  m.hi = -2048; m.big *= 2; m.s = 7;
  printf("%d %d %d %lld\n", m.lo, m.hi, m.s, m.big);
  Packed3 p{7, 2, 3};
  unsigned char raw; __builtin_memcpy(&raw, &p, 1);
  printf("%d %d %d raw %d size %zu\n", p.x, p.y, p.z, raw, sizeof(p));
  for (int i = 0; i < 10; i++) p.x = p.x + 1;
  printf("x %d\n", p.x);
  return 0;
}

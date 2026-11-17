// [[no_unique_address]] as Itanium lays it out (cpp-gatefix; CP376 (a), cpp-s9): an empty potentially-overlapping member at
// offset 0 when no subobject of its type is there, else past the data size, where it takes no data size but raises the
// class's size to its offset plus its own (clang's LayoutField, setSize(max(size, offset + size))); a non-empty one's tail
// padding reused by the next member; an empty base placed past the data size likewise
#include <cstdio>
#include <cstddef>
struct E {};
struct F {};
struct P { int i; char c; P() : i(1), c(2) {} };               // not a POD for layout: its tail padding reusable
struct S1 { [[no_unique_address]] E a; [[no_unique_address]] E b; };          // b past a: size 2
struct S2 { char c; [[no_unique_address]] E e1; [[no_unique_address]] E e2; }; // e1 at 0, e2 at 1: size 2
struct S3 : E { [[no_unique_address]] E e; };                                   // e past the base: size 2
struct S4 { [[no_unique_address]] E a; [[no_unique_address]] F f; int i; };     // both at 0: size 4
struct S5 { [[no_unique_address]] P p; char d; };                               // d in p's tail padding: size 8
struct S6 { [[no_unique_address]] E a; [[no_unique_address]] E b; [[no_unique_address]] E c; }; // 0, 1, 2: size 3
struct S7 : S1 { char x; };   // S1 is empty (only empty potentially-overlapping members): at 0, x at 0, the size S1's 2
int main() {
  int bad = 0;
  if (sizeof(S1) != 2 || offsetof(S1, b) != 1) bad |= 1;
  if (sizeof(S2) != 2 || offsetof(S2, e1) != 0 || offsetof(S2, e2) != 1) bad |= 2;
  if (sizeof(S3) != 2) bad |= 4;
  if (sizeof(S4) != 4 || offsetof(S4, f) != 0 || offsetof(S4, i) != 0) bad |= 8;
  if (sizeof(S5) != 8 || offsetof(S5, d) != 5) bad |= 16;
  if (sizeof(S6) != 3 || offsetof(S6, c) != 2) bad |= 32;
  if (sizeof(S7) != 2 || offsetof(S7, x) != 0) bad |= 64;
  S5 s5; s5.d = 9;
  if (s5.p.i != 1 || s5.p.c != 2 || s5.d != 9) bad |= 128;
  S7 s7; s7.x = 5;
  std::printf("%d %d %d %d %d %d %d %d\n", (int)sizeof(S1), (int)sizeof(S2), (int)sizeof(S3), (int)sizeof(S4), (int)sizeof(S5),
              (int)sizeof(S6), (int)sizeof(S7), bad);
  return bad;
}

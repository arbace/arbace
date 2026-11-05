// bit-fields: a packet header with fields across storage units, enumerations and bool in bit-fields, a class with
// bit-fields and a constructor, comparisons of bit-fields, a bit-field in a union
#include <cstdio>
enum Kind : unsigned { Data = 1, Ack = 2, Nack = 3 };
struct Header { unsigned version : 4; Kind kind : 2; bool urgent : 1; unsigned length : 12; unsigned short checksum : 9; };
struct Reg { unsigned lo : 16; unsigned hi : 16; Reg(unsigned v) : lo(v & 0xffff), hi(v >> 16) {} unsigned value() const { return hi << 16 | lo; } };
union Word { unsigned raw; struct { unsigned low : 8; unsigned mid : 16; unsigned top : 8; } parts; };
int main() {
  Header h{};
  h.version = 2; h.kind = Ack; h.urgent = true; h.length = 4000; h.checksum = 511;
  printf("%u %d %d %u %u size %zu\n", h.version, (int)h.kind, (int)h.urgent, h.length, h.checksum, sizeof(Header));
  h.length += 100; h.checksum++;
  printf("%u %u %d\n", h.length, h.checksum, h.kind == Ack);
  Reg r(0x12345678);
  printf("%x %x %x\n", r.lo, r.hi, r.value());
  Word w; w.raw = 0xAABBCCDD;
  printf("%x %x %x\n", w.parts.low, w.parts.mid, w.parts.top);
  w.parts.mid = 0x1234;
  printf("%x\n", w.raw);
  return 0;
}

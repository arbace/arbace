// G5c: the bit counts and byte swaps (clang's CGBuiltin: llvm.ctpop, llvm.ctlz and llvm.cttz with zero undefined, the
// generic forms' fallback; llvm.bswap) and <bit>'s functions over them (std::__has_single_bit: libsupc++'s new_opa.cc)
#include <cstdio>
#include <bit>
int main() {
  unsigned u = 0xF0F0u; unsigned long l = 0x8000000000000001ul; unsigned long long ll = 6;
  printf("%d %d %d\n", __builtin_popcount(u), __builtin_popcountl(l), __builtin_popcountll(ll));
  printf("%d %d %d\n", __builtin_clz(u), __builtin_clzl(l), __builtin_clzll(ll));
  printf("%d %d %d\n", __builtin_ctz(u), __builtin_ctzl(l), __builtin_ctzll(ll));
  unsigned char c = 0x10; unsigned short s = 0x0100;
  printf("%d %d %d\n", __builtin_popcountg(c), __builtin_clzg(s), __builtin_ctzg(s));
  printf("%d %d\n", __builtin_clzg(0u, 99), __builtin_ctzg((unsigned char)0, -1));
  printf("%x %x %lx\n", __builtin_bswap16(0x1234), __builtin_bswap32(0x12345678u), __builtin_bswap64(0x0102030405060708ul));
  // (the internal functions libsupc++ calls; the public ones' _If_is_unsigned_integer return types are G5b's template corner)
  printf("%d %d %d\n", std::__has_single_bit(64u), std::__has_single_bit(65u), std::__popcount(255u));
  printf("%d %d %u\n", std::__countl_zero(1u), std::__countr_zero(8u), std::__bit_ceil(17u));
  return 0;
}

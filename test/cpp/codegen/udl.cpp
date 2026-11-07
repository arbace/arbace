// G5: user-defined literals ([lex.ext]): the literal operator called with an integer's value (unsigned long long), a
// floating one's (long double), a character, a string and its length; mangled li <source-name>
#include <cstdio>
#include <cstddef>
unsigned long long operator""_k(unsigned long long v) { return v * 1000; }
long double operator""_h(long double v) { return v / 2; }
int operator""_c(char c) { return c + 1; }
std::size_t operator""_n(const char*, std::size_t n) { return n; }
int main() { printf("%llu %g %d %zu %llu\n", 3_k, (double)5.0_h, 'a'_c, "hello"_n, 0x10_k); return 0; }

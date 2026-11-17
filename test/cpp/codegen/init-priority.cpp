// G5: dynamic initialization's order: __attribute__((init_priority(N))) a function _GLOBAL__I_00000N of its own, the
// functions __attribute__((constructor(N))) names first in llvm.global_ctors (clang's order at equal priorities)
#include <cstdio>
struct A { const char* n; A(const char* s) : n(s) { std::printf("init %s\n", s); } };
A b("b");
__attribute__((init_priority(200))) A a("a");
__attribute__((constructor(300))) static void later() { std::printf("later\n"); }
__attribute__((constructor)) static void plain() { std::printf("plain\n"); }
int main() { std::printf("main %s %s\n", a.n, b.n); return 0; }

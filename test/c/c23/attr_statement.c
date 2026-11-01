// 6.8.1: an attribute before a statement that is not an expression statement:
// a jump, a block, a labeled statement.
int g(int x);
int f(int x) { [[gnu::musttail]] return g(x); }
void h(int x) { if (x) [[likely]] { x++; } [[maybe_unused]] l: x--; }

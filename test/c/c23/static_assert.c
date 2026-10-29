// 6.7.12: static_assert is a keyword (C11's _Static_assert).
static_assert(sizeof(int) >= 2, "int");
struct s { int a; static_assert(sizeof(int) >= 2, "member"); };
void f(void) { static_assert(1, "block"); }

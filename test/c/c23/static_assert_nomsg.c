// 6.7.12: static_assert without a message.
static_assert(sizeof(int) >= 2);
struct s { int a; static_assert(sizeof(int) >= 2); };
void f(void) { static_assert(1); }

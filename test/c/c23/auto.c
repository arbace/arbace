// 6.7.10: auto infers an object's type from its initializer.
static auto s = 2.0;
void f(void) { auto x = 1; auto p = &x; (void)p; }

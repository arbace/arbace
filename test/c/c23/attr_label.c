// 6.8.2: an attribute on a label, before a declaration.
void f(int x) { [[maybe_unused]] l: int y = x; (void)y; }

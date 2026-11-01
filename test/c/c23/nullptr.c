// 6.4.4.6: nullptr, the null pointer constant of type nullptr_t.
int *p = nullptr;
typeof(nullptr) q;
int f(void) { return p == nullptr; }

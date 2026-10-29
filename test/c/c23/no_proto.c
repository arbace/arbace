// 6.7.7.4: an empty parameter list is (void).
int f();
int f() { return 1; }
int g(void) { return f(); }

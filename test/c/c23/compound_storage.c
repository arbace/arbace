// 6.5.3.6: storage-class specifiers in a compound literal.
int *f(void) { return (static int[]){1, 2}; }
int g(void) { return (constexpr int){3}; }
int h(void) { return (register int){4}; }

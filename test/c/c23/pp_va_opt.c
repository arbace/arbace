// 6.10.5.1: __VA_OPT__ expands its tokens only when variable arguments are present.
int f(int, ...);
#define CALL(a, ...) f(a __VA_OPT__(,) __VA_ARGS__)
int g(void) { return CALL(1) + CALL(2, 3, 4); }

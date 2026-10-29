// 6.7.2: constexpr objects, usable in constant expressions.
constexpr int N = 4;
int a[N];
static constexpr double D = 1.5;
void f(void) { constexpr long L = N * 2; (void)L; }

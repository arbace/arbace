// a namespace-scope variable's initializer folded as clang's EvaluateAsInitializer folds it (cpp-90): a const double with a
// constant initializer read in another's, so the second is initialized statically, before a dynamic initialization reads it
#include <cstdio>
static const double a = 1.5;
struct S { S(); double seen; };
static S s;
static const double b = a * 2.0 + 1.0;
static const float c = (float)b / 2;
S::S() : seen(b) {}
int main() { printf("%.2f %.2f %.2f\n", s.seen, b, (double)c); }

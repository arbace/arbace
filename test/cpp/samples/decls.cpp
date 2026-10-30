struct A { explicit A(int); A(const A&) = default; A& operator=(const A&) = delete; operator int() const; constexpr int g() const noexcept { return 1; } static int s; mutable int m; int bf : 3; };
struct C final { virtual void f() final; };
int A::s = 0;
extern "C" int cf(int);
extern "C" { int cg(); }
namespace a::b { inline namespace c { int z; } }
namespace { int anon; }
using namespace a::b;
using std_int = int;
template<class T> using Al = T*;
int f(int x = 3, ...);
void (*fp)(int);
char *p = new char[3]{1,2,3};
int g(int n) { int r = 0; L: r++; if (r < n) goto L; return r; }
static_assert(sizeof(int) == 4, "x");
struct S2 { int a, b; } s2, *ps2;
typedef struct { int q; } TS;

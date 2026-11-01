struct B { int b; virtual void f(); };
struct D : B { char c; double d; void f() override; };
struct F { unsigned a:3; unsigned b:5; char c; };
namespace n { template<class T> struct G { T x; }; }
D gd; F gf; n::G<int> gg;

struct B { int b; B(int x) : b(x) {} virtual ~B() {} virtual int f() const = 0; };
class D : public B { public: D() : B(1), c{2} {} int f() const override; private: int c; static const int k = 3; friend int g(); };
int D::f() const { return c; }
enum E { A, Bv = 3 }; enum class F : char { X };
typedef int T; using U = int;
template<class X, int N = 2> struct V { X a[N]; };
template<> struct V<char, 1> { };
int main() {
  int *p = new int(3); int *q = new int[4]; D *d = new D; delete p; delete[] q;
  int v[] = {1,2};
  for (int x : v) { (void)x; }
  if (int y = 2; y > 1) return 1; else { }
  for (int i = 0; i < 3; ++i) ;
  switch (v[0]) { case 1: case 2: break; default: ; }
  try { throw 1; } catch (int e) { } catch (...) { }
  V<int> w; w.a[0] = 1;
  return 0;
}

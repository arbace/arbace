// DECISION 505-2 (a) (queue 505, 8c2fa2b4d's parser mark :_elab-decl): a class first named by an elaborated-type-specifier of a
// member's or a parameter's declaration is declared in the smallest enclosing namespace or block scope, [basic.scope.pdecl]/7.2,
// and mangled there: clang 23.1.5 (by hand, the oracle tier's tool) defines _ZN1N1T1fEPNS_1UE, _ZNK1N1S3getEv, _ZN1N1gEPNS_1VE,
// _ZN1N2mkERNS_1TE, _Z3useR1W and _ZN1X1hEv: N::S, not N::T::S; ::X, not W::X
namespace N {
  struct T { struct S *a; int f(struct U *u); };
  void g(struct V *v);
  struct S { int x; int get() const { return x; } };
  struct U { int y; };
  struct V { int z; };
  int T::f(U *u) { return a->get() + u->y; }
  void g(V *v) { v->z = 1; }
  S *mk(T &t) { return t.a; }
}
struct W { struct X *p; };
struct X { void h() {} };
void use(W &w) { w.p->h(); }
int main() {
  N::S s{2}; N::T t{&s}; N::U u{3}; N::V v{0}; N::g(&v);
  X x; W w{&x}; use(w);
  return t.f(&u) + v.z - 6 + (N::mk(t) == &s ? 0 : 1);
}

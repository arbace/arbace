// n-cpp-concepts R16: a member's trailing requires-clause names the class's members ([expr.prim.this]/2); a constrained
// variable template's template-id satisfies its constraints ([temp.names]/7)
template<class T> struct A {
  T val;
  static void g(int) requires requires { val.x; };
};
struct B { int x; };
struct C { };
template <class T> requires false constexpr bool v = true;
int main() {
  A<B>().g(0);
  A<C>().g(0);         // { dg-error "no member" }
  static_assert(v<void>); // { dg-error "constraint" }
}

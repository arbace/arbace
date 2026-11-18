// n-cpp-concepts R15: a requires-expression's parameter of type void fails the substitution ([expr.prim.req.general]/5)
template <class T> concept C = requires (T t) {  // { dg-error "invalid parameter" }
  t;
};
template <typename T> requires C<T> constexpr bool is_c() { return true; }
static_assert(is_c<int>(), "");
static_assert(is_c<void>(), ""); // { dg-error "no match" }

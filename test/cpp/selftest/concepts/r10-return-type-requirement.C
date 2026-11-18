// n-cpp-concepts R10: a return-type-requirement is `-> type-constraint` ([expr.prim.req.compound]/1)
template<typename T, typename U> concept Same = __is_same(T, U);
template<class T> concept Addable = requires(T x) {
  {x + x} -> T;                    // { dg-error "not a type-constraint" }
};
template<class T> concept Addable2 = requires(T x) {
  {x + x} -> Same<T>;
};

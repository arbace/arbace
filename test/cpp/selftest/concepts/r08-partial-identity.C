// n-cpp-concepts R8: a partial specialization of the primary's own arguments is more constrained than the primary
// ([temp.spec.partial.general]/9.2)
template<typename T> concept One = sizeof(T) >= 4;
template<typename T> concept Two = One<T> && sizeof(T) >= 8;
template<Two T> struct S4 { };
template<One T> struct S4<T> { };  // { dg-error "does not specialize" }
template<One T> struct S5 { };
template<Two T> struct S5<T> { };

// n-review-cc R17: a type-constraint's concept takes the parameter and its own arguments, as many as its parameters less
// those with a default argument at the least ([temp.param]/4, /14; std::three_way_comparable<T, Cat = partial_ordering>)
template<class T, class U = int> concept D = true;
template<class T, class U, class V = int> concept K = true;
template<D T> struct A { };
template<D<long> T> struct B { };
template<K T> struct C { };                     // { dg-error "wrong number of template arguments .1, should be at least 2." }
template<K<int, int, int> T> struct E { };      // { dg-error "wrong number of template arguments .4, should be at least 2." }
template<K<int> T> struct F { };

// n-cpp-concepts R4: a type-constraint's concept is a type concept ([temp.param]/4, [temp.concept]/7); a concept's name
// where a type stands wants auto after it ([dcl.spec.auto.general]/1); a constrained placeholder's non-type parameter parses
template<int N> concept Small = N < 10;
template<typename T> concept Type = true;
template<Small T> struct A { };   // { dg-error "does not constrain a type" }
template<Type T> struct B { };
template<Type auto N> void f() { }
Type x1 = 0;                       // { dg-error "expected 'auto'" }
Type auto x2 = 0;
void g() { f<0>(); }

// n-cpp-concepts R9: an atomic constraint has type bool ([temp.constr.atomic]/3)
struct X { };
int operator==(X, X) { return 0; }
template<typename T> concept C1 = (X() == X()); // { dg-error "bool" }
template<typename T> concept C2 = (sizeof(T) == 4);
template<C1 T> void h1(T) { }
template<C2 T> void h2(T) { }
void d() {
  h1(0);  // { dg-error "no matching" }
  h2(0);
}

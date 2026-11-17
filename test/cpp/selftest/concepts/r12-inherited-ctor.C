// n-cpp-concepts R12: an inherited constructor whose constraints are not satisfied is no candidate ([namespace.udecl]/3)
template<typename T> concept C = __is_class(T);
struct X { };
template<typename T> struct Base { Base(double) requires C<T> { } };
struct Ok1 : Base<X> { using Base<X>::Base; };
struct Err1 : Base<int> { using Base<int>::Base; };
int main() {
  Ok1 x1(0.0);
  Err1 x2(0.0); // { dg-error "no matching" }
}

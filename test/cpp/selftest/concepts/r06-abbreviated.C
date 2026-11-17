// n-cpp-concepts R6: an abbreviated template and a template of the same constraints are two templates (GCC's reading of
// [temp.over.link]/6), a call of both ambiguous; a redeclaration of the same form is one
template<typename T> concept C = __is_class(T);
struct X { } x;
void f1(C auto x);
template<C T> void f1(T x) { }
template<C T> void f2(T x);
template<C T> void f2(T x) { }
int main() {
  f1(x); // { dg-error "ambiguous" }
  f2(x);
}

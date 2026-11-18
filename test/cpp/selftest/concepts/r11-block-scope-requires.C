// n-cpp-concepts R11: a block-scope function declaration takes no requires-clause, in a template too ([dcl.decl.general]/4)
template<typename T> concept C = true;
template<typename T> void F() {
  void bad() requires C<T>;        // { dg-error "non-templated function" }
}
template<typename T> void G() requires C<T> { }

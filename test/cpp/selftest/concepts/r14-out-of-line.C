// n-cpp-concepts R14: an out-of-line member of a class template repeats its template-head's constraints
// ([temp.mem.func]/1, [temp.over.link]/6)
template<class T> concept C = true;
template<C T> requires true
struct A {
  void f();
  void g();
  static int D;
};
template<C T>
void A<T>::f() { }                 // { dg-error "different constraints" }
template<C U> requires true
void A<U>::g() { }
template<C T> requires true
int A<T>::D = 0;

// templates as members: a class nested in a class template, a member class template, a constructor template,
// a static member function template of a class
#include <cstdio>
template <class T> struct List {
  struct Node { T v; Node* next; };
  Node* head;
  List() : head(0) {}
  ~List() { while (head) { Node* n = head->next; delete head; head = n; } }
  void push(T x) { Node* n = new Node; n->v = x; n->next = head; head = n; }
  int len() const { int k = 0; for (Node* p = head; p; p = p->next) k++; return k; }
};
template <class A, class B> struct Pair {
  A a; B b;
  Pair(A x, B y) : a(x), b(y) {}
  template <class C, class D> Pair(const Pair<C, D>& o) : a(o.a), b(o.b) {}
};
struct Util { template <class T> static T twice(T t) { return t + t; } template <class T> struct In { T v; }; };
template <class T> struct Outer { template <class U> struct Inner { T t; U u; T get() const { return t + (T)u; } }; };
int main() {
  List<int> l; l.push(1); l.push(2); l.push(3);
  Pair<int, char> p(1, 'a');
  Pair<long, int> q(p);
  Util::In<int> in = {3};
  Outer<int>::Inner<double> oi = {4, 1.5};
  printf("%d %ld %d %d %g %d %d\n", l.len(), q.a, q.b, Util::twice(5), Util::twice(0.5), in.v, oi.get());
  return 0;
}

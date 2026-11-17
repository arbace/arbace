// a class template owning memory: constructor, destructor, copy constructor, members on use only
#include <cstdio>
template <class T> class Stack {
  T* data;
  int n, cap;
public:
  Stack(int c) : data(new T[c]), n(0), cap(c) {}
  Stack(const Stack& o) : data(new T[o.cap]), n(o.n), cap(o.cap) { for (int i = 0; i < n; i++) data[i] = o.data[i]; }
  ~Stack() { delete[] data; printf("free %d\n", cap); }
  void push(const T& x) { if (n < cap) data[n++] = x; }
  T pop() { return data[--n]; }
  int size() const { return n; }
  void never_used() { data->no_such_member(); }
};
int main() {
  Stack<int> s(4);
  s.push(1); s.push(2); s.push(3);
  Stack<int> t(s);
  printf("%d %d\n", s.pop(), t.size());
  Stack<double> d(2);
  d.push(0.5);
  printf("%g\n", d.pop());
  return 0;
}

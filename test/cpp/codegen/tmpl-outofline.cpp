// members of a class template defined outside it, with other parameter names than the class's
#include <cstdio>
template <class T, int N> struct Ring {
  T items[N];
  int head, n;
  Ring();
  void push(T x);
  T pop();
  bool empty() const;
};
template <class T, int N> Ring<T, N>::Ring() : head(0), n(0) {}
template <class U, int M> void Ring<U, M>::push(U x) { items[(head + n) % M] = x; if (n < M) n++; else head = (head + 1) % M; }
template <class T, int N> T Ring<T, N>::pop() { T x = items[head]; head = (head + 1) % N; n--; return x; }
template <class T, int N> bool Ring<T, N>::empty() const { return n == 0; }
int main() {
  Ring<int, 3> r;
  for (int i = 1; i <= 5; i++) r.push(i * 10);
  while (!r.empty()) printf("%d ", r.pop());
  printf("\n");
  return 0;
}

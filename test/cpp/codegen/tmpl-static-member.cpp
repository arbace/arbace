// static data members of class templates: defined outside the class, one per specialization; one initialized
// dynamically (its guard variable), an in-class constant read through an object (folded, not odr-used)
#include <cstdio>
template <class T> struct Counter { static int count; static const int base = 10; Counter() { count++; } };
template <class T> int Counter<T>::count = 0;
static int calls = 0;
int next_id() { return ++calls * 100; }
template <class T> struct Id { static int id; static const int k = 7; };
template <class T> int Id<T>::id = next_id();
int main() {
  Counter<int> a, b, c;
  Counter<double> d;
  printf("%d %d %d\n", Counter<int>::count, Counter<double>::count, Counter<char>::base);
  Id<char> ic;
  printf("%d %d %d %d\n", Id<int>::id, Id<long>::id, Id<int>::id, ic.k);
  return 0;
}

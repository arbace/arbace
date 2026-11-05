// static data members of class templates: defined outside the class, one per specialization
#include <cstdio>
template <class T> struct Counter { static int count; static const int base = 10; Counter() { count++; } };
template <class T> int Counter<T>::count = 0;
int main() {
  Counter<int> a, b, c;
  Counter<double> d;
  printf("%d %d %d\n", Counter<int>::count, Counter<double>::count, Counter<char>::base);
  return 0;
}

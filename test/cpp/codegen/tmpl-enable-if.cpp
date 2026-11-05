// enable_if in a return type and in a default template argument: overloads chosen by substitution failure
#include <cstdio>
template <bool B, class T = void> struct enable_if {};
template <class T> struct enable_if<true, T> { typedef T type; };
template <class T> struct is_int { static const bool value = false; };
template <> struct is_int<int> { static const bool value = true; };
template <> struct is_int<long> { static const bool value = true; };
template <class T> typename enable_if<is_int<T>::value, int>::type kind(T) { return 1; }
template <class T> typename enable_if<!is_int<T>::value, int>::type kind(T) { return 2; }
template <class T, class = typename enable_if<is_int<T>::value>::type> T twice(T t) { return t * 2; }
int main() {
  printf("%d %d %d %d\n", kind(1), kind(2L), kind(1.5), kind('c'));
  printf("%d %ld\n", twice(4), twice(5L));
  return 0;
}

// simple type traits as class templates (the shape of <type_traits>'s): constants, partial specializations, nested typedefs
#include <cstdio>
template <class T, T v> struct constant { static const T value = v; };
template <class T> struct is_pointer : constant<bool, false> {};
template <class T> struct is_pointer<T*> : constant<bool, true> {};
template <class T, class U> struct is_same : constant<bool, false> {};
template <class T> struct is_same<T, T> : constant<bool, true> {};
template <class T> struct remove_const { typedef T type; };
template <class T> struct remove_const<const T> { typedef T type; };
int main() {
  remove_const<const int>::type x = 3;
  x++;
  printf("%d %d %d %d %d\n", (int)is_pointer<int*>::value, (int)is_pointer<int>::value, (int)is_same<int, int>::value,
         (int)is_same<int, long>::value, x);
  return 0;
}

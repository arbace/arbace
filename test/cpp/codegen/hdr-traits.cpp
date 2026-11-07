// G4: <type_traits>' common traits, each a class template over a type-trait builtin of clang's (__is_same,
// __is_class, __is_pointer, __remove_cv, __remove_reference, __decay, __is_constructible, __is_convertible, ...)
#include <cstdio>
#include <type_traits>
struct A { int x; };
struct B : A { virtual ~B() {} };
enum E { e0 };
union U { int i; float f; };
template <class T> int show() { return std::is_integral<T>::value * 100 + std::is_pointer<T>::value * 10 + std::is_class<T>::value; }
int main() {
  std::printf("%d %d %d\n", (int)std::true_type::value, (int)std::false_type::value, (int)std::integral_constant<int, 7>::value);
  std::printf("%d %d\n", (int)std::is_same<int, int>::value, (int)std::is_same<int, const int>::value);
  std::printf("%d %d %d\n", show<int>(), show<int*>(), show<A>());
  std::printf("%d %d %d\n", (int)std::is_same<std::remove_cv<const volatile int>::type, int>::value,
              (int)std::is_same<std::remove_reference<int&&>::type, int>::value,
              (int)std::is_same<std::decay<const int[3]>::type, const int*>::value);
  std::printf("%d %d %d %d\n", (int)std::is_enum<E>::value, (int)std::is_union<U>::value, (int)std::is_base_of<A, B>::value,
              (int)std::is_polymorphic<B>::value);
  std::printf("%d %d %d\n", (int)std::is_convertible<B*, A*>::value, (int)std::is_convertible<A*, B*>::value,
              (int)std::is_constructible<A, A>::value);
  std::printf("%d %d %d\n", (int)std::is_same<std::add_pointer<int>::type, int*>::value,
              (int)std::is_same<std::conditional<true, int, long>::type, int>::value, (int)std::is_reference<int&>::value);
  std::printf("%d %d\n", (int)std::is_trivially_copyable<A>::value, (int)std::is_trivially_copyable<B>::value);
  return 0;
}

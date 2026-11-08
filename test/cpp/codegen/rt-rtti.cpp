// G5c: the runtime's RTTI (libsupc++'s tinfo, class_type_info, si_class_type_info, vmi_class_type_info, dyncast,
// pointer_type_info, pbase_type_info, fundamental_type_info, enum_type_info, function_type_info,
// array_type_info): dynamic_cast down, across and to void, through virtual bases; typeid's names and equality;
// catching through pointer conversions (qualification, to void*, to a base), a function pointer (a pointer to member's type_info is G5b's: P20)
#include <cstdio>
#include <typeinfo>
struct A { virtual ~A() {} int a = 1; };
struct B : virtual A { int b = 2; };
struct C : virtual A { int c = 3; };
struct D : B, C { int d = 4; };
struct X { virtual void f() {} };
struct Y : X, A {};
enum E { e0, e1 };
struct S { int m; };
static void fn(int) {}
int main() {
  D dd; A* pa = &dd;
  std::printf("%d %d %d\n", dynamic_cast<D*>(pa)->d, dynamic_cast<C*>(pa)->c, dynamic_cast<B*>(pa)->b);
  std::printf("%d\n", dynamic_cast<void*>(pa) == static_cast<void*>(&dd));
  Y y; X* px = &y;
  std::printf("%d %d\n", dynamic_cast<A*>(px)->a, dynamic_cast<D*>(px) == nullptr);
  std::printf("%s %s %s %s\n", typeid(dd).name(), typeid(*pa).name(), typeid(int).name(), typeid(E).name());
  std::printf("%s %s\n", typeid(const char*).name(), typeid(void (*)(int)).name());
  std::printf("%d %d\n", typeid(*pa) == typeid(D), typeid(int) == typeid(long));
  std::printf("%d\n", typeid(int[3]) == typeid(int[3]));
  try { throw &dd; } catch (A* p) { std::printf("A* %d\n", p->a); }
  try { throw (char*)"s"; } catch (const char* p) { std::printf("const char* %s\n", p); }
  try { throw (int*)nullptr; } catch (void* p) { std::printf("void* %d\n", p == nullptr); }
  try { throw e1; } catch (E e) { std::printf("enum %d\n", (int)e); }
  try { throw &fn; } catch (void (*f)(int)) { std::printf("fn %d\n", f == &fn); }
  try { throw 3L; } catch (int) { std::printf("wrong\n"); } catch (long l) { std::printf("long %ld\n", l); }
  return 0;
}

// G5c: the runtime's exception handling as a program uses it (libsupc++'s eh_alloc, eh_throw, eh_catch, eh_globals,
// eh_type, eh_exception, eh_aux_runtime): throw and catch by type, by base, by pointer, catch (...),
// rethrow, a nested try, an exception during unwinding caught, uncaught_exceptions in a destructor, the current
// exception's type, std::exception's what(), the bad_* exceptions the runtime throws
#include <cstdio>
#include <exception>
#include <new>
#include <typeinfo>
#include <stdexcept>
#include <cxxabi.h>
struct B { virtual ~B() {} int b = 1; };
struct D : B { int d = 2; };
struct G { ~G() { std::printf("~G %d\n", std::uncaught_exceptions()); } };
static int depth(int n) { G g; if (n == 0) throw D(); return depth(n - 1) + 1; }
int main() {
  try { throw 42; } catch (int x) { std::printf("int %d\n", x); }
  try { throw D(); } catch (B& b) { std::printf("base %d %s\n", b.b, typeid(b).name()); }
  try { throw new D(); } catch (B* p) { std::printf("ptr %d\n", static_cast<D*>(p)->d); delete p; }
  try { try { throw 'c'; } catch (...) { std::printf("inner\n"); throw; } } catch (char c) { std::printf("rethrown %c\n", c); }
  try { depth(2); } catch (const D& d) { std::printf("unwound %d\n", d.d); }
  try { throw std::runtime_error("boom"); } catch (const std::exception& e) { std::printf("what %s\n", e.what()); }
  try { try { throw 1.5; } catch (...) { std::type_info* t = abi::__cxa_current_exception_type(); std::printf("type %s\n", t->name()); throw; } }
  catch (double d) { std::printf("double %g\n", d); }
  { B b; B* pb = &b; std::printf("dyncast %d\n", dynamic_cast<D*>(pb) == nullptr); }
  try { throw std::bad_cast(); } catch (const std::exception& e) { std::printf("bad_cast %s\n", e.what()); }
  try { B* p = nullptr; (void)typeid(*p); } catch (const std::bad_typeid& e) { std::printf("bad_typeid %s\n", e.what()); }
  try { throw std::bad_alloc(); } catch (const std::exception& e) { std::printf("%s\n", e.what()); }
  std::printf("%d\n", std::uncaught_exceptions());
  return 0;
}

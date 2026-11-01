// CX7: the runtime's parts beyond the exit's program: function-local statics (guards), operator new and delete (nothrow,
// aligned, arrays), the library's throws through its own frames into the program (bad_alloc, bad_cast from use_facet,
// bad_variant_access, bad_function_call, bad_any_cast, ios_base::failure), a program's virtual that throws while libstdc++
// calls it (a streambuf's overflow), nested exceptions, dynamic_cast across a hierarchy, typeid of fundamental types, the
// locale in "C", std::stringstream, std::to_chars and std::from_chars, std::format.
#include <any>
#include <charconv>
#include <cstdio>
#include <format>
#include <functional>
#include <iostream>
#include <locale>
#include <new>
#include <sstream>
#include <typeinfo>
#include <variant>
#include <vector>
struct Counter { int n = 0; Counter() { std::puts("Counter()"); } };
static Counter& counter() { static Counter c; ++c.n; return c; }
struct Throwing : std::streambuf { int_type overflow(int_type) override { throw std::runtime_error("overflow"); } };
struct A { virtual ~A() = default; }; struct B : A {}; struct C : B {}; struct X { virtual ~X() = default; };
struct alignas(64) Wide { char c[64]; };
int main() {
  counter(); counter(); std::printf("counter %d\n", counter().n);
  int* p = new (std::nothrow) int[4]{1, 2, 3, 4}; std::printf("%d\n", p[3]); delete[] p;
  Wide* w = new Wide; std::printf("aligned %d\n", int(reinterpret_cast<unsigned long>(w) % 64)); delete w;
  try { std::vector<char> v; v.reserve(v.max_size() + 1); } catch (const std::length_error& e) { std::puts("length_error"); }
  try { (void)::operator new(~std::size_t(0) >> 1); } catch (const std::bad_alloc& e) { std::printf("bad_alloc %s\n", e.what()); }
  try { (void)std::use_facet<std::num_put<char, char*>>(std::locale()); } catch (const std::bad_cast& e) { std::puts("bad_cast"); }
  try { std::variant<int, double> v = 1; (void)std::get<double>(v); } catch (const std::bad_variant_access& e) { std::printf("%s\n", e.what()); }
  try { std::function<int()> f; f(); } catch (const std::bad_function_call& e) { std::printf("%s\n", e.what()); }
  try { std::any a = 1; (void)std::any_cast<float>(a); } catch (const std::bad_any_cast& e) { std::printf("%s\n", e.what()); }
  { Throwing sb; std::ostream os(&sb); os.exceptions(std::ios::badbit);
    try { os << "x" << std::flush; } catch (const std::runtime_error& e) { std::printf("callback %s\n", e.what()); } }
  { std::istringstream in("abc"); in.exceptions(std::ios::failbit); int i;
    try { in >> i; } catch (const std::ios_base::failure& e) { std::puts("ios_base::failure"); } }
  try { try { throw 1; } catch (...) { std::throw_with_nested(std::runtime_error("outer")); } }
  catch (const std::runtime_error& e) { std::printf("%s\n", e.what());
    try { std::rethrow_if_nested(e); } catch (int i) { std::printf("nested %d\n", i); } }
  A* a = new C; std::printf("%d %d %d\n", dynamic_cast<B*>(a) != nullptr, dynamic_cast<X*>(a) != nullptr, typeid(*a) == typeid(C)); delete a;
  std::printf("%s %s %s\n", typeid(int).name(), typeid(const char*).name(), typeid(std::vector<int>).name());
  std::printf("locale %s\n", std::locale().name().c_str());
  std::stringstream ss; ss << 1.5 << ' ' << -7 << ' ' << std::boolalpha << true; std::printf("%s\n", ss.str().c_str());
  char buf[32]; auto r = std::to_chars(buf, buf + 32, 3.141592653589793); *r.ptr = 0; std::printf("%s\n", buf);
  double d = 0; std::from_chars(buf, r.ptr, d); std::printf("%.15g\n", d);
  std::cout << std::format("{:>6}|{:x}|{}", "ab", 255, 2.5) << '\n';
  return 0;
}

// The native parser's subset (CX11): one construct of each kind it reads, the forms checked against the oracle's reading.
#include <compare>
#include <initializer_list>
#include <new>
#include <typeinfo>
namespace geo {
inline namespace v1 {
struct Point { int x = 0, y = 0; };
}
namespace detail { int counter; }
namespace alias = detail;
}
using geo::Point;
using namespace geo::detail;
enum Color { red, green = 3, blue };
enum class Dir : unsigned char { up = 1, down };
enum struct Opaque : int;
enum Unsized : long;
class Base {
public:
  Base() {}
  explicit Base(int v) : v_(v) {}
  virtual ~Base() {}
  virtual int get() const { return v_; }
  int v_ = 1;
protected:
  static int count;
private:
  mutable int cache_ : 4;
};
int Base::count = 0;
struct Left : virtual Base { };
struct Right : virtual public Base { };
struct Diamond final : Left, private Right {
  using Base::get;
  int get() const override { return 2; }
  Diamond& operator=(const Diamond&) & noexcept = default;
  operator bool() const { return true; }
  friend bool operator==(const Diamond&, const Diamond&);
  friend class Helper;
};
template<class T> constexpr T pi = T(3.14159);
template<class T, class U = T> struct Pair { T first; U second; };
template<class T> struct Pair<T, T*> { T only; };
template<class T> using Ptr = T*;
template<class T> concept Addable = requires(T a, T b) { a + b; };
template<class T> requires Addable<T> T add(T a, T b) { return a + b; }
template<Addable T> T twice(T a) { return a + a; }
auto triple(Addable auto a) { return a + a + a; }
template<class T> T neg(T a) requires (sizeof(T) > 1) { return -a; }
template<class... Ts> constexpr unsigned long count_of(Ts... ts) { return sizeof...(Ts); }
template<class F, class... A> auto call(F f, A... a) { return f(a...); }
struct Agg { int a; double b; char c; };
struct Defaults { Defaults() = default; Defaults(const Defaults&) = delete; Defaults& operator=(Defaults&&) = default; ~Defaults() = default; };
int main() {
  Point p{1, 2};
  Point q = {.x = 3, .y = 4};
  Agg g{.a = 1, .b = 2.0, .c = 'c'};
  int arr[3] = {1, 2, 3};
  int zero{};
  auto [px, py] = p;
  auto& [a0, a1, a2] = arr;
  for (int i = 0; auto& e : arr) e += i;
  for (const auto& [k, v] : {Pair<int>{1, 2}}) (void)k;
  int n = 2;
  auto inc = [n](int x) mutable { return x + n++; };
  auto ref = [&n, m = n * 2]() -> int { return n + m; };
  auto gen = [](auto x) { return x; };
  auto tl = []<class T>(T x) { return x; };
  int r = [] { return 7; }();
  Base* b = new Diamond;
  Diamond* d = dynamic_cast<Diamond*>(b);
  const Base* cb = static_cast<const Base*>(b);
  Base* mb = const_cast<Base*>(cb);
  long addr = reinterpret_cast<long>(b);
  int (Base::*pm) = &Base::v_;
  int (Base::*pf)() const = &Base::get;
  int via = (b->*pf)() + (*b).*pm;
  bool cmp = (1 <=> 2) < 0;
  unsigned long sz = sizeof(Base) + alignof(Agg) + sizeof n;
  bool ne = noexcept(add(1, 2));
  const char* tn = typeid(*b).name();
  int* buf = new int[n]{};
  ::delete[] buf;
  void* raw = ::operator new(16);
  Agg* placed = new (raw) Agg{1, 2.0, 'x'};
  ::operator delete(raw);
  int x = n > 1 ? n : -n, y = (x, n), w = x << 2 | y & 1 ^ ~x;
  x += 1; x -= 1; x *= 2; x /= 2; x %= 3; x <<= 1; x >>= 1; x &= 7; x |= 8; x ^= 1;
  try { if (!d) throw Base(1); } catch (const Base& e) { return e.get(); } catch (...) { throw; }
  delete b;
  return add(1, 2) + twice(3) + triple(1) + neg(2) + int(count_of(1, 2)) + call(gen, 5) + tl(1) + inc(1) + ref() + r + via
         + (cmp ? 1 : 0) + int(sz) + ne + (tn != nullptr) + int(addr != 0) + (mb == b) + int(pi<double>) + x + y + w
         + placed->a + px + py + a0 + a1 + a2 + q.x + g.a + zero + red + green + int(Dir::down) + counter;
}

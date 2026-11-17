// namespaces: nesting, reopening, qualified names, using-declarations and directives, aliases, the unnamed namespace
#include <cstdio>
namespace geo {
  int scale = 3;
  int area(int w, int h) { return w * h * scale; }
  namespace detail { int helper(int x) { return x + 1; } }
  struct Box { int w; int volume() const; };
}
namespace geo { int perimeter(int w, int h) { return 2 * (w + h); } }
int geo::Box::volume() const { return w * w * detail::helper(w); }
namespace { int hidden(int x) { return x * 100; } }
namespace a::b::c { int deep() { return 42; } }
namespace abc = a::b::c;
static int file_local = 11;
int main() {
  printf("%d %d\n", geo::area(2, 3), geo::perimeter(2, 3));
  using geo::detail::helper;
  printf("%d\n", helper(4));
  {
    using namespace geo;
    printf("%d %d\n", area(1, 1), scale);
  }
  geo::Box b{2};
  printf("%d %d %d %d\n", b.volume(), hidden(2), abc::deep(), ::file_local);
  return 0;
}

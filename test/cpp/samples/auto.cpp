namespace n { int g; template<class T> T id(T x) { return x; } }
auto f() { return 1; }
auto h() -> int { return 2; }
int main() {
  auto x = 1; const auto &y = x; auto l = [&](int a) { return a + x; };
  int a[3] = {1, 2, 3};
  unsigned u = 3u;
  return n::id<int>(x) + ::n::g + l(2) + a[1] + (int)u + static_cast<int>(2.5);
}

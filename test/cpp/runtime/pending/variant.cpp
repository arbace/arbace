// CP363 (a), cpp-s5: library.cpp's variant slice as a run sample of its own (the probe .tmp/p/v1.cpp of cpp-s4), waiting here
// for the oracle tier to record g++'s run and clang's IR (doc/cpp/CPP-SURVEY.md, "As built: cpp-s5"), then moved to ../samples/.
#include <variant>
#include <cstdio>
int main() {
  try { std::variant<int, double> v = 1; (void)std::get<double>(v); } catch (const std::bad_variant_access& e) { std::printf("%s\n", e.what()); }
  return 0;
}

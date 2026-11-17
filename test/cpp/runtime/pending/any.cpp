// CP363 (a), cpp-s5: library.cpp's any slice as a run sample of its own (the probe .tmp/p/a1.cpp of cpp-s4), waiting here
// for the oracle tier to record g++'s run and clang's IR (doc/cpp/CPP-SURVEY.md, "As built: cpp-s5"), then moved to ../samples/.
#include <any>
#include <cstdio>
int main() {
  try { std::any a = 1; (void)std::any_cast<float>(a); } catch (const std::bad_any_cast& e) { std::printf("%s\n", e.what()); }
  return 0;
}

// CX7: an exception that no handler catches: std::terminate's verbose handler (libsupc++'s vterminate.cc, lowered) prints its
// type and what() on stderr and aborts (status 134), after the cleanups of the frames it left (LLVM.md §8: no search phase).
#include <cstdio>
#include <stdexcept>
static void f() { throw std::logic_error("no handler"); }
int main() {
  std::puts("before");
  std::fflush(stdout);
  f();
  std::puts("not reached");
}

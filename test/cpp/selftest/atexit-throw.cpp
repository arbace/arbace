// An exception that leaves a function registered with atexit (CP160 (b)): g++'s build calls std::terminate at the throw
// (status 134, "terminate called after throwing an instance of 'int'"); the lowered program's registration is the pending-flag
// layer's cx$atexit, whose trampoline calls cx$uncaught when the function returns with the flag set.
#include <cstdio>
#include <cstdlib>

static void bye() {
  std::fputs("bye\n", stderr);
  throw 7;
}

int main() {
  std::fputs("main\n", stderr);
  std::atexit(bye);
  return 0;
}

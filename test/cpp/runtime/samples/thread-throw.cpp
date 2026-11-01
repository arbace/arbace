// An exception that leaves a std::thread's callable (CP120 (b)): g++'s build calls std::terminate at the throw ("terminate
// called after throwing an instance of 'std::runtime_error'", status 134), so main never prints its second line. The lowered
// program's thread start is the pending-flag layer's trampoline (cx$pthread_create), which calls cx$uncaught when the routine
// returns with the flag set.
#include <cstdio>
#include <stdexcept>
#include <thread>

int main() {
  std::fputs("main\n", stderr);
  std::thread t([] {
    std::fputs("thread\n", stderr);
    throw std::runtime_error("from a thread");
  });
  t.join();
  std::fputs("joined\n", stderr);
}

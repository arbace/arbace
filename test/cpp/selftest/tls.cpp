// cpp-threads (2026-11-15): four threads throw and catch at once, each checking a thread_local of its own across yields. The pending
// flag and the in-flight exception of the lowered runtime (arbace/cpp/rt/eh.c.ae's cx$pending, cx$cur) and this thread_local are
// _Thread_local: each thread its own (ci's threads, de576f46b). Built by an older ci (one object for every thread) a thread takes
// another's exception: test/cpp/runtime/samples/threads.cpp's fault, then its terminate from this_thread::yield.
#include <thread>
#include <cstdio>

static thread_local int mine = -1;
static int results[4];

static int work(int id) {
  int got = 0;
  mine = id;
  for (int i = 0; i < 3000; i++) {
    try {
      if (i % 2 == 0) throw id * 100000 + i;
    } catch (int v) {
      if (v != id * 100000 + i) return -1;
      got++;
    }
    std::this_thread::yield();
    if (mine != id) return -2;
  }
  return got;
}

int main() {
  std::thread a([] { results[0] = work(0); });
  std::thread b([] { results[1] = work(1); });
  std::thread c([] { results[2] = work(2); });
  std::thread d([] { results[3] = work(3); });
  a.join(); b.join(); c.join(); d.join();
  std::printf("%d %d %d %d\n", results[0], results[1], results[2], results[3]);
  return results[0] == 1500 && results[1] == 1500 && results[2] == 1500 && results[3] == 1500 ? 0 : 1;
}

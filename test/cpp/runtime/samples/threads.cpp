// Four threads throw and catch at once (CP103 (b), CP110 (c)): the pending flag and the in-flight exception are per thread.
#include <thread>
#include <mutex>
#include <stdexcept>
#include <string>
#include <cstdio>

static std::mutex mu;
static int results[4];

static int work(int id) {
  int caught = 0;
  for (int i = 0; i < 20000; i++) {
    try {
      if (i % 4 == id) throw std::runtime_error("t" + std::to_string(id));
      if (i % 5 == 0) throw i;
    } catch (const std::runtime_error& e) {
      if (e.what()[1] - '0' != id) return -1;
      caught++;
    } catch (int v) {
      if (v != i) return -2;
      caught += 2;
    }
    std::this_thread::yield();
  }
  return caught;
}

int main() {
  std::thread a([] { results[0] = work(0); });
  std::thread b([] { results[1] = work(1); });
  std::thread c([] { results[2] = work(2); });
  std::thread d([] { results[3] = work(3); });
  a.join();
  b.join();
  c.join();
  d.join();
  std::lock_guard<std::mutex> g(mu);
  std::printf("%d %d %d %d\n", results[0], results[1], results[2], results[3]);
  return results[0] > 0 && results[1] > 0 && results[2] > 0 && results[3] > 0 ? 0 : 1;
}

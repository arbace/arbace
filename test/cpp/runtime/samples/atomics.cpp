// State shared across threads (CP121 (c)): a std::atomic<int> counter and a compare-exchange loop under 4 threads, a
// std::shared_ptr copied in every thread (its refcount is libstdc++'s atomic add), a function-local static initialized by
// the threads at once (the guard's __cxa_guard_acquire, libsupc++'s, lowered) and fences. The lowered atomics are gcc's
// __atomic_* builtins with the IR's orderings: with plain operations the counts race and come out short.
#include <atomic>
#include <cstdio>
#include <memory>
#include <thread>
#include <vector>

static std::atomic<int> counter{0};
static std::atomic<long> maxv{0};
static std::atomic<unsigned char> flags{0};
static std::atomic<int> inits{0};

struct Slow {
  int v;
  Slow() : v(42) { inits.fetch_add(1); std::this_thread::yield(); }
};

static int local_static() {
  static Slow s;
  return s.v;
}

int main() {
  auto shared = std::make_shared<std::vector<int>>(100, 1);
  std::weak_ptr<std::vector<int>> weak = shared;
  std::vector<std::thread> ts;
  std::atomic<bool> go{false};
  long sums[4] = {};
  for (int id = 0; id < 4; id++)
    ts.emplace_back([&, id] {
      while (!go.load(std::memory_order_acquire)) std::this_thread::yield();
      long s = local_static();
      for (int i = 0; i < 100000; i++) {
        counter.fetch_add(1, std::memory_order_relaxed);
        std::shared_ptr<std::vector<int>> copy = shared;
        if (i % 1000 == 0) s += (*copy)[i % 100];
        long m = maxv.load();
        while (i > m && !maxv.compare_exchange_weak(m, i)) {}
        std::atomic_thread_fence(std::memory_order_seq_cst);
      }
      flags.fetch_or(1 << id);
      counter.fetch_sub(id, std::memory_order_release);
      sums[id] = s;
    });
  go.store(true, std::memory_order_release);
  for (auto& t : ts) t.join();
  std::printf("counter %d\n", counter.load());
  std::printf("max %ld flags %d inits %d\n", maxv.load(), int(flags.exchange(0)), inits.load());
  std::printf("sums %ld %ld %ld %ld\n", sums[0], sums[1], sums[2], sums[3]);
  std::printf("use_count %ld expired %d\n", shared.use_count(), int(weak.expired()));
  shared.reset();
  std::printf("expired %d\n", int(weak.expired()));
}

// G5c: the runtime's guards and handlers (libsupc++'s guard.cc, atexit_thread, new_op*, del_op*, new_handler,
// eh_terminate, vterminate, pure, eh_ptr, nested_exception): static locals initialized once (a throwing initializer
// tried again), operator new and delete in their forms, the new-handler, exception_ptr, nested exceptions,
// set_terminate's handler
#include <cstdio>
#include <cstdlib>
#include <exception>
#include <new>
static int calls = 0;
static int make(bool fail) { ++calls; if (fail) throw 1; return 7; }
static int once(bool fail) { static int v = make(fail); return v; }
struct alignas(64) Big { char c[64]; };
static int handled = 0;
static void handler() { ++handled; std::set_new_handler(nullptr); }
int main() {
  try { once(true); } catch (int) { std::printf("init threw\n"); }
  std::printf("%d %d %d\n", once(false), once(false), calls);
  int* p = new int(3); delete p;
  int* a = new int[4](); a[3] = 2; std::printf("%d\n", a[3]); delete[] a;
  int* q = new (std::nothrow) int(5); std::printf("%d\n", *q); delete q;
  Big* b = new Big; std::printf("%d\n", (int)(reinterpret_cast<unsigned long>(b) % 64)); delete b;
  Big* bs = new Big[2]; delete[] bs;
  std::set_new_handler(handler);
  try { void* x = ::operator new((size_t)-1 / 2); std::printf("no throw %p\n", x); } catch (const std::bad_alloc&) { std::printf("bad_alloc %d\n", handled); }
  std::exception_ptr ep;
  try { throw 9; } catch (...) { ep = std::current_exception(); }
  try { std::rethrow_exception(ep); } catch (int x) { std::printf("rethrown %d\n", x); }
  std::exception_ptr e2 = std::make_exception_ptr(4.5);
  try { std::rethrow_exception(e2); } catch (double d) { std::printf("made %g\n", d); }
  try { try { throw 1; } catch (...) { std::throw_with_nested(std::exception()); } }
  catch (const std::exception& e) {
    try { std::rethrow_if_nested(e); } catch (int i) { std::printf("nested %d\n", i); }
  }
  std::set_terminate([] { std::printf("terminating\n"); std::fflush(stdout); std::_Exit(3); });
  std::terminate();
}

// G5: allocation and deallocation functions as clang's Sema finds them: a class's own operator new and delete (also
// on the unwind path of a throwing constructor), operator new[] and delete[] with the size (the cookie), an over-aligned
// class (operator new(size_t, align_val_t)), a placement new with its matching placement delete, nothrow's null check,
// a virtual destructor's deleting destructor calling the class's operator delete
#include <new>
#include <cstdio>
#include <cstdlib>
struct X {
  int v;
  X(int a) : v(a) { if (a < 0) throw a; }
  static void* operator new(std::size_t n) { printf("X new %zu\n", n); return std::malloc(n); }
  static void operator delete(void* p) { printf("X delete\n"); std::free(p); }
  static void* operator new[](std::size_t n) { printf("X new[] %zu\n", n); return std::malloc(n); }
  static void operator delete[](void* p, std::size_t n) { printf("X delete[] %zu\n", n); std::free(p); }
  ~X() {}
};
struct alignas(64) Big { int i; };
struct Arena { char buf[64]; };
void* operator new(std::size_t n, Arena& a) { printf("arena new %zu\n", n); return a.buf; }
void operator delete(void*, Arena&) { printf("arena delete\n"); }
struct T { int t; T(int a) : t(a) { if (a == 0) throw 0; } };
struct Base { virtual ~Base() {} static void operator delete(void* p, std::size_t n) { printf("Base delete %zu\n", n); std::free(p); }
              static void* operator new(std::size_t n) { return std::malloc(n); } };
struct Derived : Base { long pad[3]; ~Derived() { printf("~Derived\n"); } };
int main() {
  X* x = new X(5); printf("%d\n", x->v); delete x;
  try { new X(-1); } catch (int e) { printf("caught %d\n", e); }
  X* xs = new X[3]{1, 2, 3}; printf("%d\n", xs[2].v); delete[] xs;
  Big* b = new Big; printf("aligned %d\n", int(reinterpret_cast<unsigned long>(b) % 64)); delete b;
  Arena a; T* t = new (a) T(4); printf("%d\n", t->t);
  try { new (a) T(0); } catch (int) { printf("caught 0\n"); }
  int* n = new (std::nothrow) int(9); printf("%d\n", *n); delete n;
  Base* d = new Derived; delete d;
  return 0;
}

// A throw that leaves a dynamic initializer (review 007's finding 9): g++ and the original terminate at once, so neither
// a later constructor nor main runs; the lowered program's constructors are wrappers that call cx$uncaught when the
// pending flag is set after the initializer. Its stderr is the terminate message alone, its status 134.
#include <cstdio>
#include <stdexcept>
struct A {
  A() {
    std::fputs("A\n", stderr);
    throw std::runtime_error("from a dynamic initializer");
  }
};
__attribute__((init_priority(200))) A a;
__attribute__((constructor(300))) static void later() { std::fputs("a later constructor\n", stderr); }
int main() { std::fputs("main\n", stderr); }

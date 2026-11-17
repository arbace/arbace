// lambdas: a mutable counter, a generator kept in a struct member through its function-pointer conversion, a lambda
// over an array by reference, a lambda calling another lambda
#include <cstdio>
struct Op { const char* name; int (*fn)(int, int); };
Op make(const char* n, int (*f)(int, int)) { return Op{n, f}; }
int main() {
  int calls = 0;
  auto counter = [n = 0, &calls]() mutable { calls++; return ++n; };
  printf("%d %d %d calls %d\n", counter(), counter(), counter(), calls);
  Op ops[] = {make("add", [](int a, int b) { return a + b; }), make("mul", [](int a, int b) { return a * b; }), make("sub", [](int a, int b) { return a - b; })};
  for (const Op& o : ops) printf("%s %d\n", o.name, o.fn(6, 3));
  int data[5] = {3, 1, 4, 1, 5};
  auto sum = [&data]() { int s = 0; for (int v : data) s += v; return s; };
  auto scaledSum = [&sum](int k) { return sum() * k; };
  data[0] = 30;
  printf("%d %d\n", sum(), scaledSum(2));
  return 0;
}

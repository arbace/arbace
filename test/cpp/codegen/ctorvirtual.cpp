// virtual calls during construction and destruction dispatch to the class being built or destroyed
#include <cstdio>
struct Base {
  Base() { printf("Base sees %s\n", kind()); }
  virtual ~Base() { printf("~Base sees %s\n", kind()); }
  virtual const char *kind() const { return "base"; }
  void hello() const { printf("hello from %s\n", kind()); }
};
struct Mid : Base {
  Mid() { printf("Mid sees %s\n", kind()); }
  ~Mid() override { printf("~Mid sees %s\n", kind()); }
  const char *kind() const override { return "mid"; }
};
struct Leaf : Mid { const char *kind() const override { return "leaf"; } };
int main() {
  Leaf l;
  l.hello();
  Base *b = new Mid;
  b->hello();
  delete b;
  return 0;
}

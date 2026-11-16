// single inheritance: base constructors and destructors, members through the base, upcasts, hiding
#include <cstdio>
struct Base {
  int id;
  Base(int i) : id(i) { printf("Base(%d)\n", i); }
  ~Base() { printf("~Base(%d)\n", id); }
  int twice() const { return id * 2; }
  const char *who() const { return "base"; }
};
struct Derived : Base {
  int extra;
  Derived(int i, int e) : Base(i), extra(e) { printf("Derived(%d)\n", e); }
  ~Derived() { printf("~Derived(%d)\n", extra); }
  const char *who() const { return "derived"; }
};
struct More : Derived { More() : Derived(7, 8) {} int sum() const { return id + extra; } };
int ident(const Base &b) { return b.id; }
int main() {
  Derived d(1, 2);
  printf("%d %d %s %s\n", d.twice(), d.extra, d.who(), static_cast<Base &>(d).who());
  Base *pb = &d;
  printf("%d %s\n", pb->id, pb->who());
  More m;
  printf("%d %d %d\n", m.sum(), ident(m), m.twice());
  Derived *pd = static_cast<Derived *>(pb);
  printf("%d\n", pd->extra);
  return 0;
}

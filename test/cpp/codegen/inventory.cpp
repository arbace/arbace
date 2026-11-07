// a small program over the features of G2: an inventory with a polymorphic item hierarchy over two bases, lambdas
// as predicates, exceptions for errors, bit-field flags, a union of quantities, conversion functions
#include <cstdio>
struct OutOfStock { const char* what; int need; };
struct Priced { double price; Priced(double p) : price(p) {} virtual ~Priced() {} virtual double cost(int n) const { return price * n; } };
struct Labeled { const char* label; Labeled(const char* l) : label(l) {} virtual ~Labeled() {} virtual void show() const { printf("[%s]", label); } };
struct Item : Priced, Labeled {
  unsigned stock : 10; unsigned fragile : 1;
  Item(const char* l, double p, unsigned s, bool f) : Priced(p), Labeled(l), stock(s), fragile(f) {}
  void take(unsigned n) { if (n > stock) throw OutOfStock{label, (int)(n - stock)}; stock -= n; }
  void show() const override { printf("[%s x%u%s]", label, stock, fragile ? " fragile" : ""); }
  operator bool() const { return stock > 0; }
};
struct Bulk : Item { Bulk(const char* l, double p, unsigned s) : Item(l, p, s, false) {} double cost(int n) const override { return n >= 10 ? price * n * 0.9 : price * n; } };
int countIf(Item** items, int n, bool (*pred)(const Item&)) { int c = 0; for (int i = 0; i < n; i++) if (pred(*items[i])) c++; return c; }
int main() {
  Item glass("glass", 2.5, 4, true);
  Bulk nails("nails", 0.1, 500);
  Item lamp("lamp", 30, 1, true);
  Item* all[] = {&glass, &nails, &lamp};
  for (Item* i : all) { static_cast<Labeled*>(i)->show(); printf(" "); }
  printf("\n");
  printf("fragile %d\n", countIf(all, 3, [](const Item& i) { return i.fragile == 1; }));
  int orders[][2] = {{0, 3}, {1, 20}, {2, 2}, {0, 1}};
  for (auto& o : orders) {
    Item* it = all[o[0]];
    try { double c = static_cast<Priced*>(it)->cost(o[1]); it->take(o[1]); printf("sold %d %s for %.2f\n", o[1], it->label, c); }
    catch (const OutOfStock& e) { printf("out of %s, short by %d\n", e.what, e.need); }
  }
  int left = 0;
  for (Item* i : all) if (*i) left++;
  printf("in stock %d\n", left);
  return 0;
}

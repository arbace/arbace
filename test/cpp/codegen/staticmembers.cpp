// static data members and functions, a constant member, nested types, member functions calling each other
#include <cstdio>
struct Registry {
  struct Entry { int key; int val; };
  static const int cap = 8;
  static Entry entries[cap];
  static int used;
  static void put(int k, int v) { for (int i = 0; i < used; i++) if (entries[i].key == k) { entries[i].val = v; return; } entries[used++] = Entry{k, v}; }
  static int get(int k) { for (int i = 0; i < used; i++) if (entries[i].key == k) return entries[i].val; return -1; }
  static int sum() { int s = 0; for (int i = 0; i < used; i++) s += entries[i].val; return s; }
};
Registry::Entry Registry::entries[Registry::cap];
int Registry::used = 0;
int main() {
  Registry::put(1, 10); Registry::put(2, 20); Registry::put(1, 15);
  printf("%d %d %d %d %d\n", Registry::get(1), Registry::get(2), Registry::get(3), Registry::used, Registry::sum());
  Registry::Entry e{5, 6};
  printf("%d %d %d\n", e.key + e.val, Registry::cap, (int)sizeof(Registry::Entry));
  return 0;
}

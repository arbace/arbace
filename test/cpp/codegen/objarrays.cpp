// arrays of objects: constructed in order, destroyed in reverse at scope exit, a member array of objects
#include <cstdio>
struct Item { int n; Item() : n(0) { printf("Item()\n"); } Item(int k) : n(k) { printf("Item(%d)\n", k); } ~Item() { printf("~Item(%d)\n", n); } };
struct Bag { Item items[2]; int count = 2; };
int main() {
  {
    Item row[3] = {Item(1), 2};
    printf("%d %d %d\n", row[0].n, row[1].n, row[2].n);
  }
  {
    Bag b;
    b.items[1].n = 7;
    printf("bag %d\n", b.count);
  }
  return 0;
}

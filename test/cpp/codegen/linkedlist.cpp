// a linked list of nodes owned through new and delete, a class managing it
#include <cstdio>
struct Node { int value; Node *next; Node(int v, Node *n) : value(v), next(n) {} };
class List {
  Node *head = nullptr;
  int size = 0;
public:
  ~List() { while (head) { Node *n = head->next; printf("free %d\n", head->value); delete head; head = n; } }
  void push(int v) { head = new Node(v, head); ++size; }
  void append(int v) { Node **p = &head; while (*p) p = &(*p)->next; *p = new Node(v, nullptr); ++size; }
  bool remove(int v) {
    for (Node **p = &head; *p; p = &(*p)->next)
      if ((*p)->value == v) { Node *d = *p; *p = d->next; delete d; --size; return true; }
    return false;
  }
  void reverse() { Node *prev = nullptr; while (head) { Node *n = head->next; head->next = prev; prev = head; head = n; } head = prev; }
  int sum() const { int s = 0; for (Node *n = head; n; n = n->next) s += n->value; return s; }
  void print() const { for (Node *n = head; n; n = n->next) printf("%d ", n->value); printf("(%d)\n", size); }
};
int main() {
  List l;
  for (int i = 1; i <= 5; i++) l.push(i);
  l.append(10);
  l.print();
  l.remove(3); l.remove(42);
  l.reverse();
  l.print();
  printf("sum %d\n", l.sum());
  return 0;
}

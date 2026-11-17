// conversion functions: in a switch, in a condition with && and ||, as an array index, through a base reference,
// a conversion to a pointer used with ->, and an explicit conversion by static_cast in a comparison
#include <cstdio>
struct Level { int v; operator int() const { return v; } };
struct Flag { bool on; explicit operator bool() const { return on; } };
struct Node { int val; };
struct Ref { Node* p; operator Node*() const { return p; } };
int main() {
  Level l{2};
  switch (l) { case 1: printf("one\n"); break; case 2: printf("two\n"); break; default: printf("other\n"); }
  Flag a{true}, b{false};
  if (a && !b) printf("a and not b\n");
  if (b || a) printf("b or a\n");
  int table[] = {10, 20, 30};
  printf("%d\n", table[l]);
  Node n{42};
  Ref r{&n};
  Node* np = r;
  printf("%d %d\n", np->val, static_cast<bool>(a) == true);
  int total = 0;
  Level levels[] = {Level{1}, Level{2}, Level{3}};
  for (Level x : levels) total += x;
  printf("%d\n", total);
  return 0;
}

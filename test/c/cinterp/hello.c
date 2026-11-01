#include <stdio.h>
#include <stdlib.h>
#include <string.h>
struct node { struct node *next; int v; };
int main(int argc, char **argv) {
  struct node *h = 0;
  for (int i = 0; i < 5; i++) { struct node *n = malloc(sizeof *n); n->v = i * i; n->next = h; h = n; }
  int sum = 0; for (struct node *p = h; p; p = p->next) sum += p->v;
  printf("sum=%d argc=%d %s\n", sum, argc, argc > 1 ? argv[1] : "none");
  fprintf(stderr, "to stderr\n");
  return sum % 7;
}

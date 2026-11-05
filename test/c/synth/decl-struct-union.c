// synth-c23/decl-struct-union.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// structures and unions: members, nesting, anonymous members, flexible array member, tags
// ISO/IEC 9899:2024 6.7.3.2; Annex A: struct-or-union-specifier, struct-or-union, member-declaration-list, member-declaration, specifier-qualifier-list, member-declarator-list, member-declarator
#include <stdio.h>
#include <stdlib.h>

struct node;
struct node { int v; struct node *next; };
struct outer { int a; struct { int b, c; }; union { int d; float e; }; struct inner { int f; } in; };
union u { int i; unsigned char bytes[4]; };
struct flex { int n; int data[]; };

int main(void) {
  struct node n2 = {2, 0}, n1 = {1, &n2};
  struct outer o = {.a = 1, .b = 2, .c = 3, .d = 4, .in = {5}};
  union u un = {.i = 0};
  un.bytes[0] = 0;
  struct flex *fx = malloc(sizeof(struct flex) + 3 * sizeof(int));
  fx->n = 3;
  for (int i = 0; i < 3; i++) fx->data[i] = i * i;
  struct inner in2 = o.in;
  printf("%s = %d\n", "n1.next->v", n1.next->v);
  printf("%s = %d\n", "o.a + o.b + o.c + o.d + o.in.f", o.a + o.b + o.c + o.d + o.in.f);
  printf("%s = %d\n", "un.i", un.i);
  printf("%s = %d\n", "fx->data[2]", fx->data[2]);
  printf("%s = %d\n", "in2.f", in2.f);
  printf("%s = %d\n", "sizeof(struct flex) == sizeof(int)", sizeof(struct flex) == sizeof(int));
  return 0;
}

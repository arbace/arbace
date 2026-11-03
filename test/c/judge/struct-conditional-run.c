/* queue 178 (CX8's finding 2): a conditional whose arms are structures or unions (6.5.16): the builder refused it
   ("a conversion from struct pt to struct pt"). Arms of objects, calls and compound literals, a member of the
   result, the result assigned and passed; only the chosen arm evaluated.
   Self-checking: status 0 when ci agrees with gcc 15.2, 1..N the first check that failed. */
struct pt { int x, y; };
union u { int i; float f; };
static int calls;
static struct pt mk(int x, int y) { calls++; return (struct pt){x, y}; }
static int sum(struct pt p) { return p.x + p.y; }
int main(void) {
  struct pt a = {1, 2}, b = {3, 4};
  int c = 0;
  struct pt r = c ? a : b;
  if (r.x != 3 || r.y != 4) return 1;
  r = !c ? a : b;
  if (r.x != 1) return 2;
  struct pt s = c ? mk(5, 6) : mk(7, 8);
  if (s.x != 7 || calls != 1) return 3;
  if ((c ? a : b).y != 4) return 4;
  if (sum(c ? (struct pt){10, 20} : a) != 3) return 5;
  union u ua = {.i = 9}, ub = {.i = 11};
  union u ur = c ? ua : ub;
  if (ur.i != 11) return 6;
  const struct pt *cp = &a;
  struct pt t = c ? b : *cp;
  if (t.y != 2) return 7;
  return 0;
}

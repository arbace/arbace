// if, while, do, for, switch with fall-through, break, continue, goto
#include <cstdio>
int classify(int n) {
  switch (n % 4) {
  case 0: return 10;
  case 1:
  case 2: n += 100; break;
  default: n = -n;
  }
  return n;
}
int main() {
  int sum = 0;
  for (int i = 0; i < 20; i++) {
    if (i % 3 == 0) continue;
    if (i > 15) break;
    sum += i;
  }
  printf("sum %d\n", sum);
  int n = 0; while (n < 100) n = n * 2 + 1;
  printf("n %d\n", n);
  int k = 10; do { k -= 3; } while (k > 0);
  printf("k %d\n", k);
  for (int i = 0; i < 6; ++i) printf("%d ", classify(i));
  printf("\n");
  int j = 0;
again:
  j++;
  if (j < 5) goto again;
  printf("j %d\n", j);
  bool t = true && !(j == 3) || false;
  printf("%d %s\n", t, j > 2 ? "big" : "small");
  int z = 0;
  for (;;) { if (++z == 7) break; }
  printf("z %d\n", z);
  return sum % 7;
}

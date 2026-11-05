// references: parameters, results, const references to temporaries, references as members
#include <cstdio>
int &pick(int &a, int &b, bool first) { return first ? a : b; }
void swap(int &a, int &b) { int t = a; a = b; b = t; }
const int &larger(const int &a, const int &b) { return a > b ? a : b; }
struct Holder { int &r; Holder(int &x) : r(x) {} void bump() { r *= 2; } };
int main() {
  int x = 1, y = 2;
  pick(x, y, false) = 20;
  printf("%d %d\n", x, y);
  swap(x, y);
  printf("%d %d\n", x, y);
  const int &t = 5 + x;
  printf("%d %d\n", t, larger(3, 9));
  Holder h(x); h.bump(); h.bump();
  printf("%d\n", x);
  int arr[3] = {1, 2, 3};
  int (&ra)[3] = arr;
  ra[1] = 7;
  for (int &e : arr) e += 1;
  for (int e : arr) printf("%d ", e);
  printf("\n");
  double d = 2.5; double &rd = d; rd += 1;
  printf("%.1f\n", d);
  return 0;
}

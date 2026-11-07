// algorithms over arrays: insertion sort, quicksort by recursion, binary search, a comparator by function pointer
#include <cstdio>
void isort(int *a, int n) { for (int i = 1; i < n; i++) { int v = a[i], j = i - 1; while (j >= 0 && a[j] > v) { a[j + 1] = a[j]; j--; } a[j + 1] = v; } }
void qsort2(int *a, int lo, int hi, bool (*less)(int, int)) {
  if (lo >= hi) return;
  int p = a[(lo + hi) / 2], i = lo, j = hi;
  while (i <= j) { while (less(a[i], p)) i++; while (less(p, a[j])) j--; if (i <= j) { int t = a[i]; a[i] = a[j]; a[j] = t; i++; j--; } }
  qsort2(a, lo, j, less); qsort2(a, i, hi, less);
}
bool asc(int x, int y) { return x < y; }
bool desc(int x, int y) { return x > y; }
int bsearch2(const int *a, int n, int key) { int lo = 0, hi = n - 1; while (lo <= hi) { int m = lo + (hi - lo) / 2; if (a[m] == key) return m; if (a[m] < key) lo = m + 1; else hi = m - 1; } return -1; }
void print(const int *a, int n) { for (int i = 0; i < n; i++) printf("%d ", a[i]); printf("\n"); }
int main() {
  int a[10] = {5, 3, 9, 1, 7, 2, 8, 6, 4, 0};
  int b[10]; for (int i = 0; i < 10; i++) b[i] = a[i] * 3 % 11;
  isort(a, 10); print(a, 10);
  qsort2(b, 0, 9, desc); print(b, 10);
  qsort2(b, 0, 9, asc); print(b, 10);
  printf("%d %d %d\n", bsearch2(a, 10, 7), bsearch2(a, 10, 11), bsearch2(b, 10, b[4]));
  return 0;
}

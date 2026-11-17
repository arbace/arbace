#include <cstdio>
constexpr int sq(int n) { return n * n; }
int arr[sq(3)];
struct S { int a[sq(2) + 1]; };
int main() {
  int loc[sq(2)];
  for (int i = 0; i < sq(2); i++) loc[i] = i;
  S s; s.a[4] = 5;
  arr[8] = 7;
  printf("%zu %zu %zu %d %d\n", sizeof(arr), sizeof(loc), sizeof(S), loc[3], s.a[4] + arr[8]);
  return 0;
}

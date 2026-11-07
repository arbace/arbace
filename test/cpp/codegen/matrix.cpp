// a matrix class holding a fixed array, operator() for elements, multiplication returning by value
#include <cstdio>
struct M3 {
  double e[3][3];
  M3() { for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) e[i][j] = i == j ? 1 : 0; }
  double &operator()(int i, int j) { return e[i][j]; }
  double get(int i, int j) const { return e[i][j]; }
  M3 operator*(const M3 &o) const {
    M3 r;
    for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) { double s = 0; for (int k = 0; k < 3; k++) s += get(i, k) * o.get(k, j); r(i, j) = s; }
    return r;
  }
  double trace() const { return e[0][0] + e[1][1] + e[2][2]; }
};
int main() {
  M3 a, b;
  for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) { a(i, j) = i + j; b(i, j) = i * j + 1; }
  M3 c = a * b;
  for (int i = 0; i < 3; i++) printf("%.0f %.0f %.0f\n", c(i, 0), c(i, 1), c(i, 2));
  M3 id;
  printf("%.0f %.0f\n", (c * id).trace(), (a * b * id).trace());
  return 0;
}

// overload resolution: exact match, promotion, conversion, const references, default arguments
#include <cstdio>
void show(int x) { printf("int %d\n", x); }
void show(double x) { printf("double %.2f\n", x); }
void show(char c) { printf("char %c\n", c); }
void show(const char *s) { printf("string %s\n", s); }
void show(long x, int base = 10) { printf("long %ld base %d\n", x, base); }
int area(int w, int h = 2) { return w * h; }
void ref(int &x) { x += 1; printf("ref %d\n", x); }
void ref(const int &x) { printf("const ref %d\n", x); }
int main() {
  show(3); show(2.5); show('z'); show("hi"); show(7L); show(7L, 16);
  short s = 4; show(s);          // promotion to int
  float f = 1.5f; show(f);       // promotion to double
  show(true);                    // promotion to int
  printf("%d %d\n", area(3), area(3, 4));
  int v = 1; ref(v); ref(5); const int cv = 9; ref(cv);
  return 0;
}

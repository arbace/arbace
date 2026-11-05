// character arrays and string handling written by hand
#include <cstdio>
int length(const char *s) { int n = 0; while (s[n]) n++; return n; }
void reverse(char *s) { int n = length(s); for (int i = 0, j = n - 1; i < j; i++, j--) { char t = s[i]; s[i] = s[j]; s[j] = t; } }
int count(const char *s, char c) { int k = 0; for (; *s; ++s) if (*s == c) ++k; return k; }
void upper(char *s) { for (; *s; s++) if (*s >= 'a' && *s <= 'z') *s = *s - 'a' + 'A'; }
int compare(const char *a, const char *b) { while (*a && *a == *b) { a++; b++; } return (unsigned char)*a - (unsigned char)*b; }
int main() {
  char buf[32] = "hello, world";
  printf("%d %d\n", length(buf), count(buf, 'o'));
  reverse(buf); printf("%s\n", buf);
  upper(buf); printf("%s\n", buf);
  printf("%d %d %d\n", compare("abc", "abd") < 0, compare("x", "x"), compare("b", "a") > 0);
  const char *lines[] = {"one", "two", "three"};
  int total = 0;
  for (const char *l : lines) total += length(l);
  printf("%d\n", total);
  char digits[11];
  for (int i = 0; i < 10; i++) digits[i] = '0' + (9 - i);
  digits[10] = 0;
  printf("%s %c\n", digits, "xyz"[1]);
  return 0;
}

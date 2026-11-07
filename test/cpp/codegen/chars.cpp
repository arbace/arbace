// characters: signedness of char, escapes, character arithmetic, a table of counts
#include <cstdio>
int main() {
  const char *text = "The quick brown fox jumps over the lazy dog\t\x41\101\n";
  int counts[26] = {};
  int other = 0;
  for (const char *p = text; *p; ++p) {
    char c = *p;
    if (c >= 'A' && c <= 'Z') c = c - 'A' + 'a';
    if (c >= 'a' && c <= 'z') counts[c - 'a']++; else other++;
  }
  for (int i = 0; i < 26; i++) if (counts[i] > 1) printf("%c%d ", 'a' + i, counts[i]);
  printf("| %d\n", other);
  char hi = (char)0xC8;
  printf("%d %d %d\n", hi, (unsigned char)hi, hi < 0);
  printf("%d %d %d %d\n", '\n', '\\', '\'', '\0');
  return 0;
}

// overloads on const: methods and reference parameters, the object's constness choosing, a nested class
#include <cstdio>
class Text {
  char data[16];
  int len;
public:
  class Cursor { const Text *t; int pos; public: Cursor(const Text *tx) : t(tx), pos(0) {} bool more() const { return pos < t->len; } char next() { return t->data[pos++]; } };
  Text(const char *s) : len(0) { while (s[len] && len < 15) { data[len] = s[len]; len++; } data[len] = 0; }
  char &at(int i) { printf("[mutable] "); return data[i]; }
  char at(int i) const { printf("[const] "); return data[i]; }
  const char *str() const { return data; }
};
void inspect(const Text &t) { printf("%c\n", t.at(0)); }
void tag(int &) { printf("lvalue\n"); }
void tag(const int &) { printf("const lvalue\n"); }
int main() {
  Text t("hello");
  t.at(0) = 'J';
  printf("%s\n", t.str());
  inspect(t);
  const Text &ct = t;
  printf("%c\n", ct.at(4));
  int v = 1; const int w = 2;
  tag(v); tag(w); tag(3);
  Text::Cursor c(&t);
  while (c.more()) printf("%c.", c.next());
  printf("\n");
  return 0;
}

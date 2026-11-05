// namespace-scope variables: constant and dynamic initialization, constructors before main, destructors after,
// static locals behind their guard
#include <cstdio>
struct Logger {
  const char *tag;
  Logger(const char *t) : tag(t) { printf("init %s\n", tag); }
  ~Logger() { printf("fini %s\n", tag); }
};
int counter = 10;
const int limit = 3;
int table[4] = {1, 2, 3, 4};
const char *greeting = "hello";
Logger first("first");
int computed = counter * 2 + table[3];
Logger second("second");
int next() { static int n = 100; return n++; }
Logger &shared() { static Logger l("shared"); return l; }
struct Conf { int a; double b; const char *c; } conf = {1, 2.5, "conf"};
int main() {
  printf("%d %d %d %s %d\n", counter, limit, table[2], greeting, computed);
  printf("%d %d %d\n", next(), next(), next());
  printf("%s %s\n", shared().tag, shared().tag);
  printf("%d %.1f %s\n", conf.a, conf.b, conf.c);
  return 0;
}

// destructors: scope exits (fall-through, break, continue, return), members and bases in reverse, temporaries
#include <cstdio>
struct Noisy {
  const char *name;
  Noisy(const char *n) : name(n) { printf("+%s\n", name); }
  ~Noisy() { printf("-%s\n", name); }
};
struct Pair { Noisy first{"first"}; Noisy second{"second"}; };
int early(int n) {
  Noisy a("a");
  if (n > 0) { Noisy b("b"); return n; }
  Noisy c("c");
  return 0;
}
Noisy make(const char *n) { return Noisy(n); }
int main() {
  {
    Noisy x("x");
    Noisy y("y");
  }
  for (int i = 0; i < 3; i++) {
    Noisy l("loop");
    if (i == 0) continue;
    if (i == 2) break;
    printf("body %d\n", i);
  }
  printf("early %d\n", early(1));
  printf("early %d\n", early(0));
  { Pair p; }
  printf("len %d\n", (int)sizeof(make("temp").name));
  const Noisy &kept = make("kept");
  printf("still %s\n", kept.name);
  return 0;
}

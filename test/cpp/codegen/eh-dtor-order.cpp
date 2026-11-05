// exceptions: a function-local static's destructor at exit, a global's dynamic initializer, the exception copied
// into a catch-by-value parameter, the handler's local objects destroyed before the exception
#include <cstdio>
struct Noisy { const char* n; Noisy(const char* n) : n(n) { printf("+%s\n", n); } Noisy(const Noisy& o) : n(o.n) { printf("copy %s\n", n); } ~Noisy() { printf("-%s\n", n); } };
Noisy global("global");
Noisy& getStatic() { static Noisy s("static"); return s; }
int main() {
  getStatic();
  try { Noisy t("thrown"); throw t; }
  catch (Noisy e) { Noisy inner("inner"); printf("in handler %s\n", e.n); }
  printf("end of main\n");
  return 0;
}

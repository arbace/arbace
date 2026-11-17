// virtual inheritance and exceptions: a constructor that throws after its virtual base and one base were built (both
// destroyed, the virtual base last), a catch of a class with a virtual base by reference to the base
#include <cstdio>
struct Log { const char* n; Log(const char* n) : n(n) { printf("+%s\n", n); } ~Log() { printf("-%s\n", n); } };
struct VB { Log l{"vb"}; virtual ~VB() {} virtual const char* what() const { return "VB"; } };
struct P : virtual VB { Log l{"p"}; };
struct Q : virtual VB { Log l{"q"}; Q(bool fail) { if (fail) throw 42; } };
struct R : P, Q { Log l{"r"}; R(bool fail) : Q(fail) {} const char* what() const override { return "R"; } };
struct Err : virtual VB { int code; Err(int c) : code(c) {} const char* what() const override { return "Err"; } };
int main() {
  try { R r(false); printf("built %s\n", r.what()); } catch (int e) { printf("caught %d\n", e); }
  try { R r(true); printf("not reached\n"); } catch (int e) { printf("caught %d\n", e); }
  try { throw Err(7); } catch (const VB& b) { printf("caught %s\n", b.what()); }
  return 0;
}

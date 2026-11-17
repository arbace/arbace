// function-try-blocks ([except.pre]/3, [except.handle]/15-16, cpp-90): a constructor's and a destructor's handler rethrows when
// control reaches its end, an ordinary function's falls through to the function's end; nested try in a handler
#include <cstdio>
struct E { int v; };
struct C {
  int n;
  C(int k) try : n(k) { if (k > 1) throw E{k}; printf("C(%d)\n", k); }
  catch (const E& e) { printf("C caught %d\n", e.v); try { throw 7; } catch (int i) { printf("inner %d\n", i); } }
};
struct D { ~D() noexcept(false) try { throw E{3}; } catch (E& e) { printf("~D caught %d\n", e.v); } };
int f(int k) try { if (k) throw E{k}; return 1; } catch (E& e) { printf("f caught %d\n", e.v); return 2; }
int main() {
  C a(1);
  try { C b(5); } catch (const E& e) { printf("main rethrown %d\n", e.v); }
  try { D d; } catch (E& e) { printf("main from ~D %d\n", e.v); }
  printf("f %d %d\n", f(0), f(4));
}

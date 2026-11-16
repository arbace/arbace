// G5c: __builtin_offsetof (offsetof; clang's OffsetOfExpr folded by ExprConstant): fields, nested members, array
// elements, a member of a base class, a member of an anonymous union (libsupc++'s dyncast.cc and eh_ptr.cc)
#include <cstddef>
#include <cstdio>
struct In { char c; double d[3]; };
struct B { int b0; long b1; };
struct S : B { char a; In in; union { short u0; long long u1; }; int last; };
struct vtable_prefix { long whole_object; const void* whole_type; const void* origin; };
int main() {
  printf("%zu %zu %zu\n", offsetof(In, c), offsetof(In, d), offsetof(In, d[2]));
  printf("%zu %zu %zu %zu\n", offsetof(S, a), offsetof(S, in.d[1]), offsetof(S, u1), offsetof(S, last));
  printf("%zu %zu\n", offsetof(S, b1), offsetof(vtable_prefix, origin));
  long o = -(long)offsetof(vtable_prefix, origin);
  printf("%ld %zu\n", o, sizeof(S));
  return 0;
}

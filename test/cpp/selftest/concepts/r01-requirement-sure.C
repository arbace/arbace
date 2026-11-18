// n-cpp-concepts R1: a requirement whose expression fails by the program's own error is false, no doubt
// ([expr.prim.req.simple]/1): a private destructor, a deleted one, a member the class lacks
template<typename T> concept C = requires (T& t) { t.~T(); };
template<typename T> concept P = requires (T t) { t.pet(); };
class S1 { ~S1() { } };
class S2 { public: ~S2() = delete; };
struct S3 { };
struct S4 { void pet(); };
template<P T> void pet(T);
int main() {
  static_assert(C<S1>, ""); // { dg-error "failed" }
  static_assert(C<S2>, ""); // { dg-error "failed" }
  static_assert(C<S3>, "");
  pet(S3{});                // { dg-error "no matching" }
  pet(S4{});
}

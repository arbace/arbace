// n-cpp-concepts: the negative control, R1 with its expectations moved (S1 and S2 unexpected, S3 expected): must fail
// ([expr.prim.req.simple]/1): a private destructor, a deleted one, a member the class lacks
template<typename T> concept C = requires (T& t) { t.~T(); };
template<typename T> concept P = requires (T t) { t.pet(); };
class S1 { ~S1() { } };
class S2 { public: ~S2() = delete; };
struct S3 { };
struct S4 { void pet(); };
template<P T> void pet(T);
int main() {
  static_assert(C<S1>, ""); 
  static_assert(C<S2>, ""); 
  static_assert(C<S3>, ""); // { dg-error "control" }
  pet(S3{});                // { dg-error "no matching" }
  pet(S4{});
}

// n-cpp-concepts R3: a type requirement naming a private member type ([expr.prim.req.type]/1, [class.access]/1)
template<typename T> concept C2 = requires { typename T::type; };
class S { using type = int; };
struct U { using type = int; };
int main() {
  static_assert(C2<S>, ""); // { dg-error "failed" }
  static_assert(C2<U>, "");
}

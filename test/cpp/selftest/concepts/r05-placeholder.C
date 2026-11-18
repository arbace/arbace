// n-cpp-concepts R5: a constrained placeholder's deduced type satisfies its constraint ([dcl.type.auto.deduct]/2)
template<typename T, typename U> concept same_as = __is_same(T, U);
template<typename T> concept Int = __is_same(T, int);
Int auto a = 0;
Int auto b = 'c';                  // { dg-error "constraints" }
Int auto f() { return 0; }
Int auto g() { return true; }      // { dg-error "constraints" }
same_as<int> auto h(auto, auto y) {
  return y;                        // { dg-error "constraints" }
}
int main() { h(0, 0); h(0, ""); }

// n-cpp-concepts R2: an atomic constraint naming T::x of a complete class without x is not satisfied ([temp.constr.atomic]/3)
template<typename T> concept HasDefault = T::is_pettable;
struct lizard { };
struct dog { static constexpr bool is_pettable = true; };
template<HasDefault T> void pet(T);
int main() {
  pet(lizard{}); // { dg-error "no matching" }
  pet(dog{});
}

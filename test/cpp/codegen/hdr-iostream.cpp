// G5: <iostream> by our route: std::cout with operator<< of a string literal, an int, a char and std::endl (a function
// template's address deduced from operator<<'s parameter), the library's explicit instantiation declarations (extern
// template class basic_ostream<char>, extern template ostream& endl(ostream&): nothing of them emitted), the So
// abbreviation, sentry (a member class defined outside its class template)
#include <iostream>
int main() {
  std::cout << "hello " << 42 << ' ' << -7L << std::endl;
  std::cout << "bye" << '\n';
  return 0;
}

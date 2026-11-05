// G4: std::string from <string>: basic_string<char> in the inline namespace __cxx11 (its defaults declared in
// <stringfwd.h>, char_traits<char> defined after), construction from a literal (the member template _M_construct
// defined out of line in basic_string.tcc), operator+=, c_str and size
#include <cstdio>
#include <string>
int main() {
  std::string s("hello");
  s += " world";
  std::printf("%s %d\n", s.c_str(), (int)s.size());
  return 0;
}

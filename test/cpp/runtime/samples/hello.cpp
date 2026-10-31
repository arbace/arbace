// CX7 (doc/cpp/CPP-SURVEY.md, "As built: CX7 (write-ahead)"): the exit's program: <iostream>, std::vector, std::string, std::map,
// std::sort, exceptions (thrown by the program and by the library: vector::at, stoi) and RTTI, std::exception_ptr.
#include <iostream>
#include <vector>
#include <string>
#include <map>
#include <algorithm>
#include <stdexcept>
#include <typeinfo>
#include <memory>
struct Base { virtual ~Base() {} virtual std::string name() const { return "Base"; } };
struct Derived : Base { std::string name() const override { return "Derived"; } };
int main() {
  std::cout << "hello world" << std::endl;
  std::vector<int> v{5, 3, 9, 1, 7};
  std::sort(v.begin(), v.end());
  for (int x : v) std::cout << x << ' ';
  std::cout << '\n';
  std::string s = "abc";
  s += std::to_string(42);
  std::cout << s << " " << s.size() << '\n';
  std::map<std::string, int> m{{"two", 2}, {"one", 1}, {"three", 3}};
  for (auto& [k, val] : m) std::cout << k << '=' << val << '\n';
  try { (void)v.at(10); } catch (const std::out_of_range& e) { std::cout << "caught out_of_range\n"; }
  try { throw std::runtime_error("boom"); } catch (const std::exception& e) { std::cout << "caught " << e.what() << '\n'; }
  std::unique_ptr<Base> b(new Derived);
  std::cout << b->name() << ' ' << typeid(*b.get()).name() << ' ' << (dynamic_cast<Derived*>(b.get()) != nullptr) << '\n';
  try { std::stoi("x"); } catch (const std::invalid_argument& e) { std::cout << "stoi: " << e.what() << '\n'; }
  std::cout << 3.25 << ' ' << 1e10 << ' ' << std::hex << 255 << std::dec << '\n';
  std::exception_ptr ep;
  try { throw 7; } catch (...) { ep = std::current_exception(); }
  try { std::rethrow_exception(ep); } catch (int i) { std::cout << "rethrown " << i << '\n'; }
  std::cerr << "to stderr\n";
  return 0;
}

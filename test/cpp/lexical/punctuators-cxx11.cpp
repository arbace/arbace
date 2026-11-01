// { dg-options "-std=c++11" }
// before C++20 <=> is <= and >; C++11 has the <:: rule
auto c = x <=> y;
std::vector<::std::string> v;
a<::b>;

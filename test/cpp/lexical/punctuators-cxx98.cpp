// { dg-options "-std=c++98" }
// before C++11 <:: is the digraph <: and :
std::vector<::std::string> v;
a<::b>;
auto c = x <=> y;

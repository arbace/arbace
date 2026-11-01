// { dg-options "-std=c++20" }
// C++'s punctuators: ::, ->*, .*, <=>, ..., the digraphs and the <:: rule.
struct S { int m; int f(); };
int S::* pm = &S::m;
int (S::*pf)() = &S::f;
auto r = a->*pm + b.*pm;
auto c = x <=> y;
auto d = x<=y;
int arr<:3:> = <%1, 2, 3%>;
std::vector<::std::string> v;
a<::b>;
%:define STR(x) %:x
%:define CAT(a, b) a %:%: b
auto s = STR(abc) CAT(p, q);
bool t = a and b or not c xor d bitand e bitor f;
auto u = compl x;
x and_eq y; x or_eq y; x xor_eq y; x not_eq y;
template<typename T, typename ... Ts> void f(T, Ts...);

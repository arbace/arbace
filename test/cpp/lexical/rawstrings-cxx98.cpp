// { dg-options "-std=c++98" }
// no raw strings before C++11
#define R2 "no"
auto a = R"x(abc)x";
auto b = R"(abc)";

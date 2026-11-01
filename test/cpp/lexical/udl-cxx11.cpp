// { dg-options "-std=c++11" }
#define MACRO suffix
auto a = 1.5_km + "text"_s + 'c'_c + "a"MACRO;
auto f = 1'000;

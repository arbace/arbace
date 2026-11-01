// { dg-options "-std=c++98" }
#define MACRO suffix
auto a = 1.5_km + "text"_s + 'c'_c + "a"MACRO + u8"x" + u"y" + U"z";

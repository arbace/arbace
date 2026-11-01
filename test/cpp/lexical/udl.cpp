// { dg-options "-std=c++17" }
#define MACRO suffix
#define PRIu64 "lu"
long double operator "" _km(long double);
auto a = 1.5_km + 12_km + 0x1F_x + 1.0e3_E;
auto b = "text"_s + u8"text"_s + L"text"_s + u"x"_s + U"x"_s + R"(raw)"_raw;
auto c = 'c'_c + L'c'_c + u8'c'_c + u'c'_c;
auto d = "text"MACRO + 'c'MACRO + "a"PRIu64;
auto e = "a" "b"_x "c";
auto f = 1'000'000 + 0b1010'1010 + 0x1'F + 1'0.5'5 + 1'e3;

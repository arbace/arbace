// { dg-options "-std=c++23" }
auto a = u8'a' + u'a' + U'a' + L'a' + 'a';
auto b = u8"a" u"a" U"a" L"a" "a";
auto u8 = 1; auto u = 2; auto U = 3; auto L = 4; auto R = 5;
auto c = u8 + u + U + L + R;
auto d = u8R + uR + UR + LR;

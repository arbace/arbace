// { dg-options "-std=c++17" }
#define X 1
auto a = R"(plain X "quoted" \n)";
auto b = R"delim(multi
line )" X
#define NOT_A_DIRECTIVE
)delim";
auto c = u8R"(utf-8)" ; auto d = uR"x(utf-16)x"; auto e = UR"(utf-32)"; auto f = LR"(wide)";
auto g = R"(a\
b)";
auto h = R"( ??= )";
auto i = R"x(  )x" X R"y()y";

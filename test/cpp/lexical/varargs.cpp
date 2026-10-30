// { dg-options "-std=c++20" }
#define F(a, ...) f(a __VA_OPT__(,) __VA_ARGS__)
#define G(...) g(__VA_ARGS__)
#define H(x, args...) h(x, ## args)
auto a = F(1) + F(1, 2) + F(1, 2, 3) + G() + G(a, b) + H(1) + H(1, 2);
#define STR(...) #__VA_ARGS__
auto b = STR(a , b "c" 'd' <:: R"(e)");
#define CONCAT(a, b) a ## b
auto c = CONCAT(operator, "") CONCAT(1, _km) CONCAT(u8, "x") CONCAT(R, "(y)") CONCAT(:, :) CONCAT(<, =) CONCAT(-, >) CONCAT(., *);

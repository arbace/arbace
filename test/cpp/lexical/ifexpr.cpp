// { dg-options "-std=c++20" }
#if true && !false && (1 and 2) && (not 0) && (1 bitand 1) && (0 or 1) && (compl 0) && (1 xor 0) && (1 not_eq 0)
ok1
#endif
#if __cplusplus >= 202002L && defined(__cplusplus) && __has_cpp_attribute(nodiscard) && __has_cpp_attribute(likely) >= 201803
ok2 __cplusplus
#endif
#if __has_include(<vector>) && !__has_include(<nonexistent_header>) && __has_include("punctuators.cpp")
ok3
#endif
#if __has_cpp_attribute(gnu::always_inline) && __has_cpp_attribute(noreturn)
ok4
#elifdef __cpp_concepts
ok5
#else
no
#endif
#if __cpp_lib_concepts || __cpp_concepts >= 201907L
ok6
#endif
#define and 1

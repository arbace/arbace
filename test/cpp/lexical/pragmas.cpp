// { dg-options "-std=gnu++17" }
#pragma once
#pragma GCC diagnostic push
#pragma GCC system_header
#define P(x) _Pragma(#x) int y;
P(GCC diagnostic ignored "-Wall")
auto s = "#pragma not a directive";
#define EMPTY
EMPTY # define NOT_A_DIRECTIVE
#ident "x"
#warning not an error
#line 100 "other.cpp"
int line = __LINE__; const char *file = __FILE__;

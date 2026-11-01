// { dg-options "-std=c++20 -fmodules-ts" }
// the module directives' header names are lexed as one token: the rejection is g++'s (no compiled module for the header unit)
#define FOO vector
export module foo.bar;
import <FOO>;

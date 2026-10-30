// { dg-options "-std=c++20" }
// without -fmodules-ts g++ reads these as ordinary tokens; macros are expanded in them
#define FOO vector
export module foo.bar;
import <FOO>;
export import "x.h";
import baz;
module;
module :private;
int import = 3; int module = 4; int export_ = 5;

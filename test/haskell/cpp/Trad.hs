{-# LANGUAGE CPP #-}
module Trad where
#include "MachDeps.h"
#define FOO 42
#define BAR(x,y) (x + y)
#if MIN_VERSION_base(4,8,0)
a = FOO
#else
a = 0
#endif
b = BAR(1, 2) -- foldl' FOO here
c = BAR(FOO,
        3)
#if __GLASGOW_HASKELL__ >= 908 && defined(linux_HOST_OS)
d = "FOO /* not */ " /* yes */ ++ "x"
#endif
{- text, not directives: an indented #, an unknown one
  #if not a directive
#!shebang
-}
e = WORD_SIZE_IN_BITS
f = __LINE__ __FILE__
-- an escaped newline spliced (a string gap's first backslash)
g = "abc\
    \\def"
#ifdef UNDEFINED
h = 1
#elif defined(FOO) || 0
h = 2
#endif
-- the multiple-include optimization: each guarded header entered once
#include "g1.h"
#include "g1.h"
#include "g2.h"
#include "g2.h"
-- a function-like macro's arguments over lines, its name before a newline
#define PAIR(a,b) (a, b)
i = PAIR(1,
         2)
j = PAIR
  (3, 4)
-- a slash-star in quotes is no comment; a prime quotes the rest of its line: foldl' /* PAIR(5,6)
k = "(/*)" `seq` 'x' `seq` PAIR(7,8)
#define STR(x) "x"
l = STR(a "b" c)
#undef PAIR
m = PAIR

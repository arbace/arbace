{-# LANGUAGE MagicHash #-}
module PostUnboxedLitPat where

f "foo"# = 1
g 1.5## = 2
h 2.5# = 3

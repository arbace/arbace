{-# LANGUAGE OverloadedStrings, NumericUnderscores, BinaryLiterals, NegativeLiterals #-}
module R17Literals where

a :: Int
a = 1_000 + 0xff + 0b11 + 0o7

b :: Double
b = 1.5e3 + 2

c :: String
c = "a" ++ 'b' : "c"

d :: Int
d = -1 + abs -2

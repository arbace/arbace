module ViewPatSig where

f :: Int -> Int
f (succ -> x :: Int) = x

g (id -> (pred -> y :: Int) :: Int) = y

h = (

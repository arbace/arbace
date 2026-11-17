module ViewPatSigWerror where

f :: Int -> Int
f (succ -> x :: Int) = x

module R01Basic (f, g, h, Pair(..), swap') where

data Pair a b = Pair a b

f :: Int -> Int
f x = x + 1 * 2 - 3

g :: [Int] -> Int
g = foldr (+) 0 . map (* 2) . filter even

h :: Int -> Int
h n = negate n + (-n) * (- 2)

swap' :: Pair a b -> Pair b a
swap' (Pair a b) = Pair b a

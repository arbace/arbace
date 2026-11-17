module R14Shadow where

import Data.List (map)

x :: Int
x = 1

f :: Int -> Int
f x = x + (let x = 2 in x) + (\x -> x) 3 where

g :: Maybe Int -> Int
g (Just x) = case x of x -> x
g Nothing = x

h :: [Int] -> [Int]
h xs = [x | x <- xs, let x' = x] ++ do { x <- xs; return x }

(+.) :: Int -> Int -> Int
a +. b = a - b
infixl 6 +.

k :: Int
k = 1 +. 2 +. 3 + 4

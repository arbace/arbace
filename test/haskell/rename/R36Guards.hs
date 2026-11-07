module R36Guards where

f :: Maybe Int -> Int
f m | Just x <- m, x > 0 = x
    | Just y <- m = negate y
f _ = 0

g :: [Int] -> Int
g xs | (a : _) <- xs, let b = a + 1, b > 2 = b
     | otherwise = 0

h :: Int -> String
h n = case () of
  _ | n < 0 -> "neg"
    | n == 0 -> "zero"
  _ -> "pos"

w :: Int -> Int
w x = y where y | x > 0 = 1
                | otherwise = 2

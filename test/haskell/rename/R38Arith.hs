module R38Arith where

a :: Double
a = 1.5 + 2 * 3.0e-2 - 4 / 2

b :: Int
b = 2 ^ 10 `div` 3 `mod` 5 + (-1)

c :: Integer
c = product [1 .. 20] - sum [1, 2, 3] * negate 4

d :: Bool
d = 1 < 2 && 2 <= 3 || not (3 > 4) && 4 >= 5 && 5 /= 6

e :: Int
e = fromIntegral (3 :: Integer) + round (2.5 :: Double) + length "ab" + abs (-3)

f :: [Int] -> [Int]
f xs = [x * 2 | x <- xs, x > 1, even x] ++ [y | y <- xs, odd y]

g :: Double -> Double
g x = x ** 2 + sqrt x - (-x) / (- 2)

module T03Defaulting where

three = 3

half = 0.5

plus = (+)

addOne x = x + 1

shown = show (2 + 3)

sumSq xs = sum (map (\x -> x * x) xs)

avg :: [Double] -> Double
avg xs = sum xs / fromIntegral (length xs)

compareLen xs ys = length xs == length ys

greet name = "hello " ++ name

neg x = negate x * 2

eqList xs = xs == [1, 2, 3]

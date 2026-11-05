module T09Local2 where

sumWith xs = go 0 xs
  where go acc [] = acc
        go acc (y : ys) = go (acc + y) ys

pairs xs = [(x, y) | x <- xs, y <- xs, x /= y]

lens = [length s | s <- ["a", "bb"]]

squares n = [i * i | i <- [1 .. n]]

annotate x = (x :: Int) + 1

polyWhere :: Show a => a -> String
polyWhere x = wrap (show x)
  where wrap s = "<" ++ s ++ ">"

mutual n = isEven n
  where isEven 0 = True
        isEven k = isOdd (k - 1)
        isOdd 0 = False
        isOdd k = isEven (k - 1)

average xs = s / fromIntegral n
  where s = sum xs
        n = length xs

maxBy f x y = if f x >= f y then x else y

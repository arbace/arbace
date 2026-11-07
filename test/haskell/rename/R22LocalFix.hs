module R22LocalFix where

f :: Int -> Int -> Int
f a b = a <+> b <+> 3 <.> 2
  where
    infixl 6 <+>
    infixl 7 <.>
    x <+> y = x + y
    x <.> y = x * y

g :: Int
g = let infixr 5 +++
        a +++ b = a - b
    in 10 +++ 3 +++ 2

h :: Int -> Int
h n = case n of
  0 -> 1
  m | even m, let k = m `div` 2, k > 1 -> k
    | otherwise -> m - 1

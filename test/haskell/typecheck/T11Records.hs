module T11Records where

data Person = Person { name :: String, age :: Int }

type Name = String

greeting :: Person -> Name
greeting p = "hi " ++ name p

older p = p { age = age p + 1 }

mk n = Person { name = n, age = 0 }

data Shape = Circle Double | Rect Double Double

area (Circle r) = pi * r * r
area (Rect w h) = w * h

describe s = case s of
  Circle r | r > 10 -> "big circle"
           | otherwise -> "circle"
  Rect _ _ -> "rect"

totalArea shapes = sum (map area shapes)

lookupAge n ps = case filter (\p -> name p == n) ps of
  (p : _) -> Just (age p)
  [] -> Nothing

ages = map age

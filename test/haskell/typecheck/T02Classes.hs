module T02Classes where

class Shape a where
  area :: a -> Double
  perimeter :: a -> Double
  perimeter _ = 0
  describe :: a -> String
  describe x = "area " ++ show (area x)

class Shape a => Solid a where
  volume :: a -> Double -> Double
  volume x hgt = area x * hgt

data Sq = Sq Double

instance Shape Sq where
  area (Sq s) = s * s
  perimeter (Sq s) = 4 * s

instance Solid Sq

data Pair a = Pair a a

instance Eq a => Eq (Pair a) where
  Pair a b == Pair c d = a == c && b == d

samePair p q = p == q

areas xs = map area xs

bigger x y = if area x > area y then describe x else describe y

total :: Solid a => a -> Double
total x = volume x 2 + area x

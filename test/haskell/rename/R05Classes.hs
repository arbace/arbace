module R05Classes where

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

class Container f where
  empty :: f a
  insert :: a -> f a -> f a
  toL :: f a -> [a]

newtype Box a = Box [a]

instance Container Box where
  empty = Box []
  insert x (Box xs) = Box (x : xs)
  toL (Box xs) = xs

instance Show Sq where
  showsPrec d (Sq s) = showParen (d > 10) (showString "Sq " . showsPrec 11 s)

instance Eq Sq where
  Sq a == Sq b = a == b
  Sq a /= Sq b = a /= b

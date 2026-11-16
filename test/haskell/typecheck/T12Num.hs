module T12Num where

newtype V2 = V2 (Int, Int)

instance Show V2 where
  showsPrec d (V2 (x, y)) = showParen (d > 10) (showString "V2 " . showsPrec 11 x . showString " " . showsPrec 11 y)

instance Num V2 where
  V2 (a, b) + V2 (c, d) = V2 (a + c, b + d)
  V2 (a, b) * V2 (c, d) = V2 (a * c, b * d)
  abs (V2 (a, b)) = V2 (abs a, abs b)
  signum (V2 (a, b)) = V2 (signum a, signum b)
  fromInteger n = V2 (fromInteger n, fromInteger n)
  negate (V2 (a, b)) = V2 (negate a, negate b)

origin :: V2
origin = 0

shifted = origin + 1

class Monoidal a where
  unit :: a
  (<+>) :: a -> a -> a

infixr 6 <+>

instance Monoidal [b] where
  unit = []
  (<+>) = (++)

instance (Monoidal a, Monoidal b) => Monoidal (a, b) where
  unit = (unit, unit)
  (a, b) <+> (c, d) = (a <+> c, b <+> d)

mconcatL xs = foldr (<+>) unit xs

pairUp = ([1 :: Int], "x") <+> unit

data Ordering3 = Lo | Mid | Hi

instance Eq Ordering3 where
  a == b = rank a == rank b

instance Ord Ordering3 where
  compare a b = compare (rank a) (rank b)

rank :: Ordering3 -> Int
rank Lo = 0
rank Mid = 1
rank Hi = 2

maxO xs = maximum xs

sorted3 = [Hi, Lo] < [Mid]

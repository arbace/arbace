module R31FixClass where

import Prelude hiding ((<>), (++))

infixr 6 <>
infixr 5 ++

class Monoid' a where
  (<>) :: a -> a -> a

instance Monoid' [b] where
  (<>) = (++)

(++) :: [a] -> [a] -> [a]
[] ++ ys = ys
(x : xs) ++ ys = x : (xs ++ ys)

t :: [Int]
t = [1] <> [2] ++ [3] <> [4]

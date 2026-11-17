module T08Classes2 where

class Container f where
  empty :: f a
  insert :: a -> f a -> f a
  toL :: f a -> [a]

newtype Stack a = Stack [a]

instance Container Stack where
  empty = Stack []
  insert x (Stack xs) = Stack (x : xs)
  toL (Stack xs) = xs

fill xs = foldr insert empty xs

stackOf :: [a] -> Stack a
stackOf = fill

class (Eq a, Show a) => Token a where
  render :: a -> String
  render x = show x ++ "!"
  same :: a -> a -> Bool
  same x y = x == y

data Color = Red | Green

instance Eq Color where
  Red == Red = True
  Green == Green = True
  _ == _ = False

instance Show Color where
  show Red = "Red"
  show Green = "Green"

instance Token Color

shout c = render c ++ render c

pairEq (a, b) (c, d) = a == c && b == d

ordered :: (Ord a, Show a) => a -> a -> String
ordered x y = if x <= y then show x else show y

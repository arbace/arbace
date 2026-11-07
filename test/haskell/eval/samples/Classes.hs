-- HE8's sample of user classes: dictionaries passed at -O0, a superclass, a default method, a polymorphic recursion
-- over a class constraint, instances for a data type and a type constructor, and a class with a functional argument.
module Main (main) where

import Data.List (foldl')

class Shape a where
  area :: a -> Double
  name :: a -> String
  describe :: a -> String
  describe x = name x ++ " of area " ++ show (area x)

data Circle = Circle Double
data Rect = Rect Double Double

instance Shape Circle where
  area (Circle r) = 3 * r * r
  name _ = "circle"

instance Shape Rect where
  area (Rect w h) = w * h
  name _ = "rect"
  describe r = "a " ++ name r ++ ", " ++ show (area r)

class Shape a => Solid a where
  volume :: a -> Double -> Double
  volume x h = area x * h

instance Solid Circle
instance Solid Rect where
  volume r h = area r * h / 2

data AnyShape = forall s. Solid s => AnyShape s

class Container f where
  empty :: f a
  insert :: a -> f a -> f a
  toL :: f a -> [a]

newtype Stack a = Stack [a]
newtype Queue a = Queue ([a], [a])

instance Container Stack where
  empty = Stack []
  insert x (Stack xs) = Stack (x : xs)
  toL (Stack xs) = xs

instance Container Queue where
  empty = Queue ([], [])
  insert x (Queue (f, b)) = Queue (f, x : b)
  toL (Queue (f, b)) = f ++ reverse b

fill :: Container f => [a] -> f a
fill = foldr insert empty

class Monoid' a where
  mempty' :: a
  mappend' :: a -> a -> a

instance Monoid' Int where
  mempty' = 0
  mappend' = (+)

instance Monoid' [b] where
  mempty' = []
  mappend' = (++)

instance (Monoid' a, Monoid' b) => Monoid' (a, b) where
  mempty' = (mempty', mempty')
  mappend' (a, b) (c, d) = (mappend' a c, mappend' b d)

mconcat' :: Monoid' a => [a] -> a
mconcat' = foldl' mappend' mempty'

-- a polymorphic recursion: each level a new dictionary
data Nested a = Flat a | Nest (Nested [a])

depth :: Show a => Nested a -> String
depth (Flat x) = show x
depth (Nest n) = depth n

sumSquares :: (Num a, Ord a) => a -> a -> a
sumSquares lo hi = go lo 0 where
  go i acc | i > hi = acc
           | otherwise = go (i + 1) (acc + i * i)

main :: IO ()
main = do
  mapM_ (putStrLn . describe) [Circle 1.5]
  putStrLn (describe (Rect 2 3.5))
  mapM_ (\(AnyShape s) -> print (volume s 4)) [AnyShape (Circle 2), AnyShape (Rect 3 4)]
  print (toL (fill [1 .. 5 :: Int] :: Stack Int))
  print (toL (fill "queue" :: Queue Char))
  print (mconcat' [(1 :: Int, "a"), (2, "b"), (39, "c")])
  putStrLn (depth (Nest (Nest (Flat [[1, 2], [3 :: Int]]))))
  print (sumSquares (1 :: Int) 1000, sumSquares (1 :: Integer) 100000, sumSquares (0.5 :: Double) 10)

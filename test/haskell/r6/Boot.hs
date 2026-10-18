module Boot (Tree (..), leaf, size) where

import User (weight)

data Tree a = Leaf a | Node [Tree a]
  deriving Show

leaf :: a -> Tree a
leaf = Leaf

size :: Tree a -> Int
size (Leaf _) = 1
size (Node ts) = sum (map weight ts)

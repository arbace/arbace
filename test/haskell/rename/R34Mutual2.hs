module R34Mutual2 where

data Tree a = Leaf | Node (Forest a) a
data Forest a = Forest [Tree a]

class Sized a where size :: a -> Int
instance Sized (Tree a) where
  size Leaf = 0
  size (Node f _) = 1 + size f
instance Sized (Forest a) where
  size (Forest ts) = sum (map size ts)

type Alias = Tree Int
newtype Wrap = Wrap Alias

isEven, isOdd :: Int -> Bool
isEven 0 = True
isEven n = isOdd (n - 1)
isOdd 0 = False
isOdd n = isEven (n - 1)

lets :: Int
lets = let p = q + 1
           q = 2
           r = r + s
           s = r
       in p + r

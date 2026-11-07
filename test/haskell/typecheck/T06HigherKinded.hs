module T06HigherKinded where

data Tree a = Leaf | Node (Tree a) a (Tree a)

instance Functor Tree where
  fmap _ Leaf = Leaf
  fmap f (Node l x r) = Node (fmap f l) (f x) (fmap f r)

insert :: Ord a => a -> Tree a -> Tree a
insert x Leaf = Node Leaf x Leaf
insert x t@(Node l y r)
  | x < y = Node (insert x l) y r
  | x > y = Node l y (insert x r)
  | otherwise = t

toList Leaf = []
toList (Node l x r) = toList l ++ [x] ++ toList r

fromList xs = foldr insert Leaf xs

sizeT t = length (toList t)

doubled t = fmap (* 2) t

newtype Box a = Box a

instance Show a => Show (Box a) where
  show (Box x) = "Box " ++ show x

boxes = map Box [1, 2, 3]

showBoxes = show (Box 'x')

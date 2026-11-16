module Main where

data Shape = Circle Double | Rect Double Double | Tri Double Double Double

area :: Shape -> Double
area (Circle r) = 3 * r * r
area (Rect w h) = w * h
area (Tri a b c) = let s = (a + b + c) / 2 in sqrt (s * (s - a) * (s - b) * (s - c))

data Tree a = Leaf | Node (Tree a) a (Tree a)

insert :: Int -> Tree Int -> Tree Int
insert x Leaf = Node Leaf x Leaf
insert x t@(Node l y r)
  | x < y = Node (insert x l) y r
  | x > y = Node l y (insert x r)
  | otherwise = t

toList :: Tree a -> [a]
toList Leaf = []
toList (Node l x r) = toList l ++ [x] ++ toList r

depth :: Tree a -> Int
depth Leaf = 0
depth (Node l _ r) = 1 + max (depth l) (depth r)

class Describe a where
  describe :: a -> String
  name :: a -> String
  name _ = "thing"

instance Describe Shape where
  describe (Circle _) = "circle"
  describe (Rect _ _) = "rectangle"
  describe _ = "triangle"

data Color = Red | Green | Blue

instance Describe Color where
  describe Red = "red"
  describe Green = "green"
  describe Blue = "blue"
  name _ = "color"

main :: IO ()
main = do
  let shapes = [Circle 1, Rect 2 3, Tri 3 4 5]
  print (map area shapes)
  mapM_ (\s -> putStrLn (name s ++ ": " ++ describe s)) shapes
  mapM_ (\c -> putStrLn (name c ++ ": " ++ describe c)) [Red, Green, Blue]
  let t = foldr insert Leaf [5, 3, 8, 1, 4, 7, 9 :: Int]
  print (toList t)
  print (depth t)
  print [x * y | x <- [1 .. 4 :: Int], odd x, y <- [x .. 4], let z = x + y, z /= 5]

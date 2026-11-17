module T01Basic where

identity x = x

compose f g x = f (g x)

twice f = compose f f

pairUp x y = (x, y)

swap (a, b) = (b, a)

len [] = 0
len (_ : xs) = 1 + len xs

mapL f [] = []
mapL f (x : xs) = f x : mapL f xs

apply :: (a -> b) -> a -> b
apply f x = f x

hd (x : _) = x

konst x _ = x

flipped = flip konst

nested x = let y = x
               g z = (y, z)
           in g True

isEmpty xs = case xs of
  [] -> True
  _ -> False

choose b x y = if b then x else y

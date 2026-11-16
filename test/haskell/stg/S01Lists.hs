module Main where

len :: [a] -> Int
len [] = 0
len (_ : xs) = 1 + len xs

mapL :: (a -> b) -> [a] -> [b]
mapL f [] = []
mapL f (x : xs) = f x : mapL f xs

swap :: (a, b) -> (b, a)
swap (a, b) = (b, a)

zipL :: [a] -> [b] -> [(a, b)]
zipL (x : xs) (y : ys) = (x, y) : zipL xs ys
zipL _ _ = []

lastL :: [a] -> Maybe a
lastL [] = Nothing
lastL [x] = Just x
lastL (_ : xs) = lastL xs

main :: IO ()
main = do
  print (len [1, 2, 3 :: Int])
  print (mapL (+ 1) [1, 2, 3 :: Int])
  print (swap (1 :: Int, 'x'))
  print (zipL "abc" [True, False])
  print (lastL [4, 5, 6 :: Int], lastL ([] :: [Int]))

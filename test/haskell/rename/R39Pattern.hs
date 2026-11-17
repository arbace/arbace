module R39Pattern where

f :: (Int, (Bool, Char)) -> Int
f (a, (True, _)) = a
f (_, (False, 'x')) = 0
f (a, _) = a

g :: [[Int]] -> Int
g [[a], [b, c]] = a + b + c
g ([] : _) = 1
g _ = 2

h :: Maybe (Either Int String) -> Int
h (Just (Left n)) = n
h (Just (Right s@(_ : _))) = length s
h _ = 0

k :: Int -> Int
k (-1) = 0
k 1 = 1
k n = n

o :: [Int] -> Int
o (x : y : rest) = x + y + o rest
o [x] = x
o [] = 0

s :: String -> Int
s ('a' : _) = 1
s "bc" = 2
s _ = 3

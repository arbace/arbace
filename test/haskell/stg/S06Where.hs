module Main where

import Data.List (sortBy, foldl')
import Data.Char (toUpper, isDigit)

qsort :: Ord a => [a] -> [a]
qsort [] = []
qsort (p : xs) = qsort smaller ++ [p] ++ qsort larger
  where
    smaller = [x | x <- xs, x < p]
    larger = [x | x <- xs, x >= p]

pairs :: [a] -> [(a, a)]
pairs xs = go xs
  where
    go (a : b : rest) = (a, b) : go rest
    go _ = []

collatz :: Int -> Int
collatz n = go n 0
  where
    go 1 acc = acc
    go m acc
      | even m = go (m `div` 2) (acc + 1)
      | otherwise = go (3 * m + 1) (acc + 1)

wordsOf :: String -> [String]
wordsOf s = case dropWhile (== ' ') s of
  "" -> []
  s' -> let (w, rest) = break (== ' ') s' in w : wordsOf rest

classify :: Integer -> String
classify 0 = "zero"
classify 1 = "one"
classify n = if n < 0 then "negative" else "many"

main :: IO ()
main = do
  print (qsort [3, 1, 4, 1, 5, 9, 2, 6 :: Int])
  print (qsort "haskell")
  print (pairs [1 .. 7 :: Int])
  print (map collatz [1, 7, 27])
  print (wordsOf "  the quick  brown fox ")
  putStrLn (map toUpper "shout")
  print (filter isDigit "a1b2c3", foldl' (+) 0 [1 .. 10 :: Int])
  print (sortBy (\a b -> compare b a) [2, 8, 5 :: Int])
  mapM_ (putStrLn . classify) [0, 1, -4, 12]
  print (let (q, r) = 17 `divMod` 5 in q * 10 + r :: Int)

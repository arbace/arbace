{-# LANGUAGE LambdaCase, MultiWayIf, TupleSections, ViewPatterns, BangPatterns #-}
module R09Expr where

import Data.Char (toUpper)

e1 :: Int -> String
e1 = \case
  0 -> "zero"
  n | n < 0 -> "neg"
    | otherwise -> if | n > 100 -> "big" | n > 10 -> "mid" | otherwise -> "small"

e2 :: [Int] -> Int
e2 xs = case xs of
  [] -> 0
  (x : rest) | x > 0 -> x + e2 rest
             | True -> e2 rest

e3 :: Maybe Int -> IO ()
e3 m = do
  x <- maybe (return 0) return m
  let y = x + 1
      z = y * 2
  print (x, y, z)
  (a, b) <- return (1 :: Int, 2 :: Int)
  mapM_ print [a, b]
  return ()

e4 :: [Int] -> [(Int, Char)]
e4 = map (, 'x')

e5 :: String -> String
e5 (map toUpper -> s) = s

e6 :: Int -> Int
e6 x = let f y = y + x in f 1 where

e7 :: Int
e7 = (\a b -> a + b) 1 2

e8 :: [Int]
e8 = [1, 3 .. 9] ++ [x | Just x <- [Just 1, Nothing]]

e9 :: (Int, Int) -> Int
e9 p@(fst -> a) = a + snd p

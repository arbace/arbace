{-# LANGUAGE ParallelListComp, TransformListComp, MonadComprehensions #-}
module R44Parallel where

h :: [Int] -> [Int] -> [(Int, Int)]
h xs ys = [(x, y) | x <- xs | y <- ys]

h2 :: [Int] -> [(Int, Int)]
h2 xs = [(x, y) | x <- xs, y <- xs, then take 3]

m :: Maybe Int
m = [x + y | x <- Just 1, y <- Just 2]

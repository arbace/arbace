{-# LANGUAGE Arrows, RecursiveDo, MonadComprehensions, TupleSections, BlockArguments, NumDecimals #-}
module G02Mdo where

import Control.Monad.Fix

a :: IO [Int]
a = mdo
  xs <- return (1 : map (+ 1) ys)
  ys <- return (take 3 xs)
  return ys

b :: Maybe Int
b = [x + y | x <- Just 1, y <- Just 2]

c :: IO ()
c = mapM_ print do
  [1, 2, 3 :: Int]

d :: [(Int, Bool)]
d = map (,True) [1, 2]

{-# LANGUAGE RecursiveDo #-}
module R45Mdo where

import Control.Monad.Fix

a :: IO [Int]
a = mdo
  xs <- return (1 : map (+ 1) ys)
  ys <- return (take 3 xs)
  print ys
  return ys

b :: Maybe [Int]
b = mdo
  Just z <- Just (Just (1 : z))
  return (take 2 z)

{-# LANGUAGE LambdaCase, ScopedTypeVariables #-}
{-# LANGUAGE TupleSections
           , MultiWayIf #-}
{-# OPTIONS_GHC -Wall -Wno-unused-imports -fno-warn-orphans -O1 #-}
{-# OPTIONS_GHC -Werror=tabs -Wno-error=star-is-type -fmax-errors=3 #-}
{-# OPTIONS_HADDOCK hide #-}
-- | A module with a rich header.
module Hdr1
  ( f
  , g
  , T (..)
  ) where

import Data.List (sortBy)
import qualified Data.Map as M

data T = A Int | B { name :: String, size :: !Int }

f :: Int -> Int
f = \case
  0 -> 1
  n -> n * f (n - 1)

g :: [Int] -> [(Int, Int)]
g xs = map (,1) (sortBy compare xs)

h :: Int -> String
h n = if | n < 0 -> "neg"
         | n == 0 -> "zero"
         | otherwise -> "pos"

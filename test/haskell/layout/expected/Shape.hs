{-# LANGUAGE LambdaCase, BangPatterns, TypeApplications, MagicHash #-}
-- | Shapes: the spec's worked example (doc/haskell/SPEC.md, section 2).
module Data.Shape
  (Shape (..)
  , area
  , (<+>)
  , classify) where


import qualified Data.Map.Strict as M
import Data.List (sortBy, foldl')
import GHC.Exts (Int (I#), (+#))

infixl 6 <+>

data Shape
  = Circle Double
  | Rect {w, h :: !Double}
  deriving (Show, Eq)

area :: Shape -> Double
area (Circle r) = pi * r ^ 2
area Rect {w = x, h} = x * h

(<+>) :: Shape -> Shape -> Double
a <+> b = area a + area b

classify :: (Ord a, Num a) => a -> String
classify n
  | n < 0 = "negative"
  | n /= 0, n > 100 = big
  | otherwise = 's' : "mall\torӒzero"
  where
    big = "big"

tally :: [Shape] -> M.Map String Int
tally = foldl' (\m s -> M.insertWith (+) (name s) 1 m) M.empty
  where
    name = \case
        Circle _ -> "circle"
        Rect {} -> "rect"

main :: IO ()
main = do
    let xs@(~(first : _)) = [Circle 1.5, Rect 2 3]
        !n = length xs
    print (map area xs, n, first)
    print [area s | s <- xs, area s > 1e3]
    print (read @Int "42", I# (1# +# 2#), 31, (`div` 2) 7, (subtract 1) 3)
    mapM_ print (sortBy (\a b -> compare (area a) (area b)) xs)

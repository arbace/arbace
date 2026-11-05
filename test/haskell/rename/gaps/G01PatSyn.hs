{-# LANGUAGE ForeignFunctionInterface, PatternSynonyms, ViewPatterns, ImplicitParams, ParallelListComp #-}
module G01PatSyn where

foreign import ccall "math.h sin" c_sin :: Double -> Double

pattern Zero :: Int
pattern Zero = 0

pattern Pair :: a -> b -> (a, b)
pattern Pair x y = (x, y)

f :: Int -> Bool
f Zero = True
f _ = False

g :: (Int, Int) -> Int
g (Pair a b) = a + b

h :: [Int] -> [Int] -> [(Int, Int)]
h xs ys = [(x, y) | x <- xs | y <- ys]

k :: (?lim :: Int) => Int -> Bool
k n = n < ?lim

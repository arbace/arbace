{-# LANGUAGE PatternSynonyms, ViewPatterns #-}
module R42PatSyn (pattern Zero, pattern Pair, pattern Head, f, g) where

pattern Zero :: Int
pattern Zero = 0

pattern Pair :: a -> b -> (a, b)
pattern Pair x y = (x, y)

pattern Head :: a -> [a]
pattern Head x <- (x : _)

f :: Int -> Bool
f Zero = True
f _ = False

g :: (Int, Int) -> Int
g (Pair a b) = a + b

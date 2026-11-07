module R18Pragmas where

{-# INLINE f #-}
f :: Int -> Int
f = (+ 1)

{-# NOINLINE g #-}
g :: Int -> Int
g = (* 2)

{-# SPECIALISE h :: Int -> Int #-}
h :: Num a => a -> a
h = (+ 1)

{-# DEPRECATED old "use f" #-}
old :: Int
old = 0

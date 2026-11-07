module PostUnallowedPragma where

x = {-# SCC "a" #-} 1 :: Int
y = {-# SCC "b" #-} f + 2
z = g + {-# SCC "c" #-} f + 2

-- The interface level's case (bin/lib/repr-haskell/test, 4; SPEC §9.3): layout/ and braces/ are
-- one module, its columns moved; changed/ is layout/ with one value changed.
module N where
import GHC.Stack
{-# INLINE g #-}
g :: Int -> Int
g 0 = 2
g 1 = error "x"
h :: HasCallStack => Int -> Int
h x = case x of { 2 -> 3 }
k :: Int -> Int
k y = h y + undefined
data R = R { f1 :: Int } | Q
j r = f1 r

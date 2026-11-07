{-# LANGUAGE ExplicitNamespaces #-}
module R11Export (module R11Export, module Data.Maybe, T(MkT), U(..), V, f1, (+++), C(m1), Maybe(Just), type (~>)) where

import Data.Maybe
import qualified Data.List as L

data T = MkT | MkT2
data U = U1 | U2 Int
data V = V1
type a ~> b = a -> b

class C a where
  m1 :: a -> Int
  m2 :: a -> Int

f1 :: Int
f1 = 1

f2 :: Int
f2 = L.length [f1]

(+++) :: Int -> Int -> Int
a +++ b = a + b

{-# LANGUAGE NoImplicitPrelude #-}
module R07NoPrelude (ident, cmp) where

import Prelude (Int, Bool(..), (<), Eq((==)))
import qualified Prelude as P

ident :: a -> a
ident x = x

cmp :: Int -> Int -> Bool
cmp a b = if a < b then True else a == b P.&& P.not P.False

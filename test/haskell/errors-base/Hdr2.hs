{-# OPTIONS_GHC "-Wall" -Wcompat #-}
{-# LANGUAGE Haskell2010 #-}
{-# LANGUAGE BangPatterns, GADTs, KindSignatures #-}
{-# OPTIONS_GHC -fwarn-tabs -Wno-name-shadowing #-}
module Hdr2 where

import Data.Kind (Type)

data E :: Type -> Type where
  I :: Int -> E Int
  P :: E a -> E b -> E (a, b)

eval :: E a -> a
eval (I n) = n
eval (P a b) = (eval a, eval b)

sumTo :: Int -> Int
sumTo n = go 0 n
  where
    go !acc 0 = acc
    go !acc k = go (acc + k) (k - 1)

-- {-# LANGUAGE Foo #-}
{- {-# OPTIONS_GHC -fbogus #-} -}

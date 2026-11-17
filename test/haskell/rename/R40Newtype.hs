{-# LANGUAGE GeneralizedNewtypeDeriving, DerivingVia, StandaloneDeriving #-}
module R40Newtype where

import Data.Monoid (Sum(..))

newtype Age = Age Int deriving (Eq, Ord, Show, Num)
newtype Total = Total Int deriving (Semigroup, Monoid) via (Sum Int)

class Default a where def :: a
instance Default Int where def = 0
instance Default Age where def = Age def

data P = P { px :: Int, py :: Int }
deriving instance Eq P
deriving instance Show P

total :: [Age] -> Age
total = foldr (+) 0

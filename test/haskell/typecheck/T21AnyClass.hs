{-# LANGUAGE DeriveAnyClass, DeriveFunctor, GeneralizedNewtypeDeriving #-}
module T21AnyClass where

class Describe a where
  describe :: a -> String
  describe _ = "thing"
  name' :: a -> Int
  name' _ = 0

data Thing = Thing deriving (Describe)

data Both a = Both a a deriving (Eq, Ord, Show, Describe, Functor)

newtype Meters = Meters Int deriving (Eq, Ord, Show, Describe)

describeAll :: (Describe a) => [a] -> [String]
describeAll = map describe

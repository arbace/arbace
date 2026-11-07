{-# LANGUAGE DerivingStrategies, GeneralizedNewtypeDeriving, DeriveAnyClass, DeriveFunctor, StandaloneDeriving #-}
module T23Strategies where

class Describe a where
  describe :: a -> String
  describe _ = "thing"

-- strategies written in the clauses
newtype Age = Age Int
  deriving stock (Show)
  deriving newtype (Eq, Ord, Num)

data P = P Int Bool
  deriving stock (Eq, Ord, Show)
  deriving anyclass (Describe)

-- the newtype strategy for Show: the representation's
newtype Name = Name String
  deriving newtype Show
  deriving newtype (Eq, Ord)

-- stock where GeneralizedNewtypeDeriving would pick the newtype strategy
newtype Id = Id Int
  deriving stock (Eq, Show)
  deriving newtype (Ord, Enum, Num, Real, Integral)

newtype Box a = Box [a]
  deriving stock (Show, Functor)
  deriving newtype (Eq, Ord, Semigroup, Monoid)

-- an anyclass instance of a newtype
newtype Tag = Tag Int
  deriving stock (Eq)
  deriving anyclass (Describe)

-- standalone deriving with strategies
data Pair a = Pair a a
newtype W a = W a
newtype F a = F [a]

deriving stock instance Eq a => Eq (Pair a)
deriving stock instance Show a => Show (Pair a)
deriving anyclass instance Describe (Pair a)
deriving newtype instance Num a => Num (W a)
deriving newtype instance Show a => Show (W a)
deriving newtype instance Functor F

total :: [Age] -> Age
total = sum

{-# LANGUAGE DerivingStrategies, GeneralizedNewtypeDeriving, StandaloneDeriving, DeriveFunctor #-}
module R15Deriving where

data Color = Red | Green | Blue deriving (Eq, Ord, Show, Read, Enum, Bounded)
newtype N = N Int deriving newtype (Eq, Ord, Num)
data Tr a = Lf | Nd (Tr a) a (Tr a) deriving stock (Show, Functor)
data Pt = Pt Int Int
deriving instance Eq Pt
deriving instance Show Pt

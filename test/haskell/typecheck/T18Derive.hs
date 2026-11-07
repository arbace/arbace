module T18Derive where

import Data.Ix

data Color = Red | Green | Blue deriving (Eq, Ord, Show, Read, Enum, Bounded, Ix)

data Pt = Pt Int Int deriving (Eq, Ord, Show, Read, Bounded, Ix)

data Shape a = Circle a | Rect a a | Empty deriving (Eq, Ord, Show, Read)

data Tree a = Leaf | Node (Tree a) a (Tree a) deriving (Eq, Ord, Show)

infixr 5 :+:
data E = Int :+: E | Nil deriving (Eq, Show, Read)

data R = R { name :: String, age :: Int } deriving (Eq, Ord, Show, Read)

newtype W = W Int deriving (Eq, Ord, Show)

data Pair a b = Pair a b deriving (Eq, Ord, Show, Bounded)

data Dir = N | E' | S | W' | Up | Down deriving (Eq, Ord, Enum, Bounded, Show)

useAll = (Red < Blue, [Red ..], show (Circle (3 :: Int)), succ Green, minBound :: Color, range (Red, Blue))
sameTree t = t == Leaf || t < Leaf

{-# LANGUAGE DeriveFunctor, DeriveFoldable, DeriveTraversable, GeneralizedNewtypeDeriving, StandaloneDeriving #-}
module T19Derive2 where

data Tree a = Leaf | Node (Tree a) a (Tree a) deriving (Eq, Show, Functor, Foldable, Traversable)

data Rose a = Rose a [Rose a] deriving (Show, Functor, Foldable, Traversable)

data Two a = Two a a deriving (Functor, Foldable, Traversable)

data Tagged t a = Tagged t a Int deriving (Functor, Foldable)

newtype Age = Age Int deriving (Eq, Ord, Show, Num)

newtype Wrap a = Wrap [a] deriving (Eq, Show, Functor, Foldable)

data Op = Plus | Minus

deriving instance Eq Op
deriving instance Show Op

data Box a = Box a | NoBox

deriving instance Show a => Show (Box a)
deriving instance Eq a => Eq (Box a)

total :: Tree Int -> Int
total = sum

incr :: Tree Int -> Tree Int
incr = fmap (+ 1)

inOrder :: Tree a -> [a]
inOrder = foldr (:) []

twice :: Age -> Age
twice a = a + a

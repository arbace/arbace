{-# LANGUAGE DeriveFunctor, DeriveFoldable, DeriveTraversable #-}
module T22Tuples where

data Assoc k a = Assoc [(k, a)] deriving (Functor, Foldable, Traversable)

data Pairs a = Pairs (a, a) [(Int, a)] (Maybe (a, Int)) deriving (Show, Functor, Foldable, Traversable)

data Triple a = Triple (Int, a, [a]) deriving (Functor, Foldable, Traversable)

sumAssoc :: Assoc String Int -> Int
sumAssoc = sum

bump :: Pairs Int -> Pairs Int
bump = fmap succ

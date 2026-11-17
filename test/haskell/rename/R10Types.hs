{-# LANGUAGE ScopedTypeVariables, RankNTypes, KindSignatures, TypeOperators, TypeFamilies, GADTs #-}
module R10Types where

import Data.Kind (Type)

type Name = String
type Id a = a -> a
type a + b = Either a b

data T (f :: Type -> Type) a = T (f a) | U { un :: a }

data G a where
  GI :: Int -> G Int
  GB :: Bool -> G Bool

class C a where
  type Elem a :: Type
  first :: a -> Elem a

instance C [b] where
  type Elem [b] = b
  first = head

type family F a where
  F Int = Bool
  F a = a

t1 :: forall a b. (a -> b) -> [a] -> [b]
t1 f = go where
  go :: [a] -> [b]
  go [] = []
  go (x : xs) = f x : go xs

t2 :: (forall x. x -> x) -> (Int, Bool)
t2 i = (i 1, i True)

t3 :: Int + Bool -> Name
t3 _ = ""

t4 :: a -> b -> a
t4 x _ = x

t5 :: Maybe a -> [b] -> (a, b)
t5 = undefined

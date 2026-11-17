{-# LANGUAGE TypeOperators, TypeFamilies, DataKinds, KindSignatures, PolyKinds #-}
module R32TypeOps where

import Data.Kind (Type)

infixr 5 :+:
infixr 6 :*:

data (f :+: g) a = L (f a) | R (g a)
data (f :*: g) a = f a :*: g a

type f ~> g = forall x. f x -> g x

type T1 = Maybe :+: [] :*: Maybe

nat :: Maybe ~> []
nat Nothing = []
nat (Just x) = [x]

data Proxy (a :: k) = Proxy

type family Elem c :: Type

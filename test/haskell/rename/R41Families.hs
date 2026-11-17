{-# LANGUAGE TypeFamilies, DataKinds, PolyKinds, FlexibleContexts, StandaloneKindSignatures, RoleAnnotations #-}
module R41Families where

import Data.Kind (Type)

data family Vec a
data instance Vec Int = VInt [Int]
newtype instance Vec Bool = VBool Integer

class Coll c where
  data Elt c
  type Key c :: Type
  type Key c = Int
  insert :: Elt c -> c -> c

instance Coll [a] where
  data Elt [a] = EL a
  insert (EL x) xs = x : xs

type family Len (xs :: [k]) :: Type where
  Len '[] = Int
  Len (x ': xs) = Bool

type T :: Type -> Type
data T a = MkT a

type role T representational


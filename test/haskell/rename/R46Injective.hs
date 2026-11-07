{-# LANGUAGE TypeFamilyDependencies, DataKinds #-}
module R46Injective where

type family Not a = res | res -> a where
  Not 'False = 'True
  Not 'True = 'False

type family Id a = r | r -> a
type instance Id Int = Int

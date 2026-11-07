{-# LANGUAGE MultiParamTypeClasses, FunctionalDependencies, FlexibleInstances, FlexibleContexts, DefaultSignatures #-}
module R24Classes2 where

infixr 5 +++

class Container f a | f -> a where
  cmap :: (a -> a) -> f -> f
  (+++) :: f -> f -> f
  infixl 6 `cmap`
  cempty :: f

class Pretty a where
  pretty :: a -> String
  default pretty :: Show a => a -> String
  pretty = show

instance Pretty Int
instance Pretty Bool where
  pretty True = "yes"
  pretty False = "no"

instance Pretty a => Pretty [a] where
  pretty xs = concatMap pretty xs

instance (Pretty a, Pretty b) => Pretty (a, b) where
  pretty (a, b) = pretty a ++ "," ++ pretty b

instance Container [Int] Int where
  cmap = map
  (+++) = (++)
  cempty = []

twice :: (Pretty a, Show a) => a -> String
twice x = pretty x ++ show x

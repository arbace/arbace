{-# LANGUAGE ExistentialQuantification, GADTs, ScopedTypeVariables, TypeApplications, KindSignatures #-}
module R25Existential where

data Showable = forall a. Show a => MkShowable a

data Ex where
  MkEx :: forall b. Show b => b -> Ex
  MkEx2 :: c -> [c] -> Ex

data T a = !a :* !a | T { tf :: {-# UNPACK #-} !Int }

f :: forall a. Show a => [a] -> [String]
f xs = map g xs
  where
    g :: a -> String
    g = show

h :: Int
h = read @Int "3"

p :: Showable -> String
p (MkShowable a) = show a

k :: [a] -> Int
k (xs :: [b]) = length (xs :: [b])

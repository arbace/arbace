-- | R5's cases (doc/haskell/HASKELL-NOTES.md, "R5"): what the other boot libraries brought, one
-- module; this documentation comes before the LANGUAGE pragma (stm's TSem).
{-# LANGUAGE CPP, GADTs, RankNTypes #-}
module Cases
  ( (?~)
  , (\\)
  , quote
  , T (..)
  , G (..)
  , C
  , f
  ) where

-- a CPP module's line that ends in \ would be joined with the next (containers' Data.Set)
infixl 9 \\ --

(\\) :: [a] -> [a] -> [a]
xs \\ _ = xs

-- an operator that begins with ?, not an implicit parameter (Cabal-syntax's Lens)
(?~) :: a -> a -> a
a ?~ _ = a

-- the variable quote, applied to one argument (Cabal's quote (prettyShow tt))
quote :: String -> String
quote s = "'" ++ s ++ "'"

-- a respelled operator that begins a line (Cabal-syntax's ^^^)
f :: String
f = quote (quote "x")
  ^^^ "y"
  where
    (^^^) :: String -> String -> String
    a ^^^ b = a ++ b

-- a constructor's documentation after its | (Cabal's SetupHooks.Errors)
data T
  = A
      Int
      -- ^ an Int
  | -- | B's documentation
    B

-- a class's context on lines of its own (Cabal-syntax's FieldGrammar.Class)
class
  ( Show a
  , Eq a
  ) => C a

-- a DEPRECATED pragma on a line of its own (Cabal's Simple.Build)
{-# DEPRECATED
  f
  "use something else"
  #-}

-- a GADT signature's forall on a later line (Cabal's SetupHooks.Rule)
data G a where
  G1
    :: forall a
     . Show a => a -> G a

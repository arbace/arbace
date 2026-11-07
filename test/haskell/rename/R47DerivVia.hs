{-# LANGUAGE DerivingVia, StandaloneDeriving #-}
module R47DerivVia where
import Data.Monoid
newtype N a = N a
deriving via (Sum Int) instance Semigroup (N Int)
deriving via (Sum a) instance Num a => Semigroup (N a)

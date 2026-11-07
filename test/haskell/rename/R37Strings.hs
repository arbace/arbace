{-# LANGUAGE OverloadedStrings, OverloadedLists #-}
module R37Strings where

import Data.String (IsString(..))

newtype N = N String deriving (Show, Eq)
instance IsString N where fromString = N

a :: N
a = "hello"

f :: N -> Int
f "x" = 1
f _ = 0

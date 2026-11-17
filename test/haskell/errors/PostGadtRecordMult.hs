{-# LANGUAGE GADTs, LinearTypes #-}
module PostGadtRecordMult where

data T where
  MkT :: { x :: Int } %1 -> T
  MkU :: { y :: Int } %m -> T
  MkV :: { z :: Int } ⊸ T

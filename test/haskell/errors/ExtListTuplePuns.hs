{-# LANGUAGE DataKinds, UnboxedTuples, UnboxedSums, NoListTuplePuns #-}
module ExtListTuplePuns where

type T1 = (# #)
type T2 = (# Int, Bool #)
type T3 = (# Int | Bool #)
type T4 = '()
type T5 = '[Int, Bool]
type T6 = '(Int, Bool)
type T7 = '[]

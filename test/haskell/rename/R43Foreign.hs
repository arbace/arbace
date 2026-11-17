{-# LANGUAGE ForeignFunctionInterface, ImplicitParams #-}
module R43Foreign (c_sin, k) where

import Foreign.C.Types (CDouble(..))

foreign import ccall unsafe "math.h sin" c_sin :: CDouble -> CDouble
foreign import ccall "math.h cos" c_cos :: Double -> Double

k :: (?lim :: Int) => Int -> Bool
k n = n < ?lim

twice :: Double -> Double
twice x = c_cos x + c_cos x

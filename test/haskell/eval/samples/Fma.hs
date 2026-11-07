{-# LANGUAGE MagicHash #-}
-- fused multiply-add of Float#: rounded once (review 189, F7: x = 1+2^-12, z = 2^-80, x*x+z is just above a halfway case)
import GHC.Exts
import GHC.Float

main :: IO ()
main = do
  let x = 1 + 2 ** (-12) :: Float
      z = 2 ** (-80) :: Float
      f (F# a) (F# b) (F# c) = F# (fmaddFloat# a b c)
      g (F# a) (F# b) (F# c) = F# (fmsubFloat# a b c)
      h (F# a) (F# b) (F# c) = F# (fnmaddFloat# a b c)
      k (F# a) (F# b) (F# c) = F# (fnmsubFloat# a b c)
  print (castFloatToWord32 (f x x z), castFloatToWord32 (f x x 0))
  print (map castFloatToWord32 [g x x (-z), h x x (-z), k x x z])

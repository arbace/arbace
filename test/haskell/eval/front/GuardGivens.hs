{-# LANGUAGE GADTs, TypeApplications, ScopedTypeVariables, TypeOperators, RankNTypes #-}
-- Review 1111, finding 2 (doc/haskell/FRONTEND-SURVEY.md, "As built: review 1111's fixes"): a pattern guard's given
-- equality (HRefl's b ~~ c) must not rewrite the wanteds of its own scrutinee (Typeable b, Typeable c) when the clause
-- already has givens (a GADT match, a signature's a ~ Int). Before the fix every comparison below printed True.
-- Run by the own front end: bin/repr-haskell run-check front/GuardGivens (REPR_HASKELL_RUN_IFACES=pack, HSI_LIBS the -O2
-- pack); its expected output GuardGivens.stdout.
module Main where

import Type.Reflection

data T a where
  TI :: T Int

-- a GADT match's given (a ~ Int), then the guard
gadt :: forall a b c. (Typeable b, Typeable c) => T a -> a -> b -> c -> Bool
gadt TI _ _ _ | Just HRefl <- typeRep @b `eqTypeRep` typeRep @c = True
gadt _ _ _ _ = False

-- a signature's given (a ~ Int), then the guard
sig :: forall a b c. (a ~ Int, Typeable b, Typeable c) => a -> b -> c -> Bool
sig _ _ _ | Just HRefl <- typeRep @b `eqTypeRep` typeRep @c = True
          | otherwise = False

main :: IO ()
main = do
  print (gadt TI 0 'x' True)
  print (gadt TI 0 'x' 'y')
  print (gadt TI 0 (1 :: Int) "s")
  print (sig 0 'x' True)
  print (sig 0 'x' 'y')
  print (sig 0 () ())

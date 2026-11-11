{-# LANGUAGE GADTs, TypeApplications, ScopedTypeVariables, TypeOperators, RankNTypes #-}
-- Review 1111, finding 2 (doc/haskell/FRONTEND-SURVEY.md, "As built: review 1111's fixes"): a pattern guard's given
-- equality (HRefl's b ~~ c) must not rewrite the wanteds of its own scrutinee (Typeable b, Typeable c) when the clause
-- already has givens (a GADT match, a signature's a ~ Int). Before the fix every comparison below printed True.
-- Review 1111b, finding 1: two equality-giving guards in one GRHS; the second guard's scrutinee (Typeable b, Typeable c)
-- is under the first guard's givens (a ~~ Int) only, not its own pattern's (b ~~ c); before the fix the second guard
-- always matched (two and twoG printed True for b, c of different types).
-- Run by the own front end in run-check's plan (bin/repr-haskell run-check, every program of samples, synthetic and
-- front; lib-stg --run): no GHC run is recorded for it, so it is judged by its own verdict, the committed
-- GuardGivens.stdout (status 0, no stderr), written by hand from GHC's semantics (GHC was not run: Type.Reflection's
-- eqTypeRep compares the two TypeReps, Just HRefl exactly when the types are equal; a failed guard falls through to the
-- next GRHS or clause).
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

-- two equality-giving guards, no clause givens: a ~~ Int, then b ~~ c
two :: forall a b c. (Typeable a, Typeable b, Typeable c) => a -> b -> c -> Bool
two _ _ _ | Just HRefl <- typeRep @a `eqTypeRep` typeRep @Int
          , Just HRefl <- typeRep @b `eqTypeRep` typeRep @c = True
          | otherwise = False

-- the same after a GADT match's given (d ~ Int)
twoG :: forall d a b c. (Typeable a, Typeable b, Typeable c) => T d -> a -> b -> c -> Bool
twoG TI _ _ _ | Just HRefl <- typeRep @a `eqTypeRep` typeRep @Int
              , Just HRefl <- typeRep @b `eqTypeRep` typeRep @c = True
twoG _ _ _ _ = False

main :: IO ()
main = do
  print (gadt TI 0 'x' True)
  print (gadt TI 0 'x' 'y')
  print (gadt TI 0 (1 :: Int) "s")
  print (sig 0 'x' True)
  print (sig 0 'x' 'y')
  print (sig 0 () ())
  print (two (0 :: Int) 'x' True)
  print (two (0 :: Int) 'x' 'y')
  print (two True 'x' 'y')
  print (twoG TI (0 :: Int) 'x' True)
  print (twoG TI (0 :: Int) () ())
  print (twoG TI "s" () ())

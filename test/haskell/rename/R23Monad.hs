module R23Monad where

import Data.Maybe (fromMaybe)

newtype State s a = State { runState :: s -> (a, s) }

instance Functor (State s) where
  fmap f (State g) = State $ \s -> let (a, s') = g s in (f a, s')

instance Applicative (State s) where
  pure a = State (\s -> (a, s))
  State f <*> State g = State $ \s ->
    let (h, s1) = f s
        (a, s2) = g s1
    in (h a, s2)

instance Monad (State s) where
  return = pure
  State g >>= f = State $ \s -> let (a, s') = g s in runState (f a) s'

get :: State s s
get = State (\s -> (s, s))

put :: s -> State s ()
put s = State (\_ -> ((), s))

counter :: State Int Int
counter = do
  n <- get
  put (n + 1)
  m <- get
  return (n + m)

instance Semigroup (State s a) where
  a <> _ = a

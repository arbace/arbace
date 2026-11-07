module T07Monad where

newtype State s a = State (s -> (a, s))

runState :: State s a -> s -> (a, s)
runState (State f) = f

instance Functor (State s) where
  fmap f (State g) = State (\s -> let (a, s') = g s in (f a, s'))

instance Applicative (State s) where
  pure a = State (\s -> (a, s))
  State f <*> State g = State (\s -> let (h, s1) = f s
                                         (a, s2) = g s1
                                     in (h a, s2))

instance Monad (State s) where
  State g >>= k = State (\s -> let (a, s1) = g s
                                   State h = k a
                               in h s1)

get = State (\s -> (s, s))

put s = State (\_ -> ((), s))

tick = do
  n <- get
  put (n + 1)
  return n

counter = runState (tick >> tick >> tick) 0

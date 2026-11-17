module Main where

newtype State s a = State { runState :: s -> (a, s) }

instance Functor (State s) where
  fmap f (State g) = State (\s -> let (a, s') = g s in (f a, s'))

instance Applicative (State s) where
  pure a = State (\s -> (a, s))
  State f <*> State g = State (\s -> let (h, s1) = f s
                                         (a, s2) = g s1
                                     in (h a, s2))

instance Monad (State s) where
  State g >>= k = State (\s -> let (a, s1) = g s in runState (k a) s1)

get :: State s s
get = State (\s -> (s, s))

put :: s -> State s ()
put s = State (\_ -> ((), s))

counter :: Int -> State Int [Int]
counter 0 = return []
counter n = do
  x <- get
  put (x + n)
  rest <- counter (n - 1)
  return (x : rest)

safeDiv :: Int -> Int -> Maybe Int
safeDiv _ 0 = Nothing
safeDiv a b = Just (a `div` b)

calc :: Int -> Int -> Int -> Maybe Int
calc a b c = do
  x <- safeDiv a b
  y <- safeDiv x c
  return (x + y)

main :: IO ()
main = do
  print (runState (counter 4) 10)
  print (calc 100 5 2, calc 1 0 3)
  mapM_ print (sequence [Just 1, Just (2 :: Int)])
  r <- return (length "abc")
  print r

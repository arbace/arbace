module T05Monad where

main :: IO ()
main = do
  putStrLn "hi"
  let n = 3
  print (n + 1)
  mapM_ print [1, 2, 3]

safeDiv :: Int -> Int -> Maybe Int
safeDiv _ 0 = Nothing
safeDiv a b = Just (a `div` b)

calc a b c = do
  x <- safeDiv a b
  y <- safeDiv x c
  return (x + y)

seqTwo m1 m2 = m1 >> m2

lifted f m = m >>= \x -> return (f x)

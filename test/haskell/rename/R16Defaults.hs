module R16Defaults where

default (Integer, Double)

import' :: Int
import' = 3

main :: IO ()
main = do
  print (2 ^ 10)
  putStrLn $ show 1.5 ++ "x"
  mapM_ (putStrLn . show) [1 :: Int ..]

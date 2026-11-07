{-# LANGUAGE ScopedTypeVariables #-}
module Main (main) where
import qualified Data.Map.Strict as M
import Data.List (foldl')
import Control.Exception
import Data.IORef

data Shape = Circle Double | Rect Double Double deriving (Show, Eq)

class Area a where area :: a -> Double
instance Area Shape where
  area (Circle r) = pi * r * r
  area (Rect w h) = w * h

primes :: [Int]
primes = sieve [2..] where sieve (p:xs) = p : sieve [x | x <- xs, x `mod` p /= 0]

main :: IO ()
main = do
  print (take 10 primes)
  print (map area [Circle 1, Rect 2 3])
  let m = foldl' (\acc k -> M.insertWith (+) (k `mod` 7) k acc) M.empty [1..100::Int]
  print (M.toList m)
  r <- newIORef (0::Integer)
  mapM_ (\i -> modifyIORef' r (+ i)) [1..1000]
  readIORef r >>= print . (^ (3::Int))
  res <- try (evaluate (head ([] :: [Int])))
  case res of
    Left (e :: SomeException) -> putStrLn "caught"
    Right v -> print v

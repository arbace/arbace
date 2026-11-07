module Main where

data Person = Person { pname :: String, age :: Int }

data V = V !Int !Int

instance Show V where
  show (V a b) = "V " ++ show a ++ " " ++ show b

addV :: V -> V -> V
addV (V a b) (V c d) = V (a + c) (b + d)

newtype Meters = Meters Double

data Complex = Complex Double Double

instance Num Complex where
  Complex a b + Complex c d = Complex (a + c) (b + d)
  Complex a b * Complex c d = Complex (a * c - b * d) (a * d + b * c)
  negate (Complex a b) = Complex (negate a) (negate b)
  abs z = z
  signum z = z
  fromInteger n = Complex (fromInteger n) 0

re, im :: Complex -> Double
re (Complex a _) = a
im (Complex _ b) = b

older :: Person -> Person
older p = p { age = age p + 1 }

describe :: Person -> String
describe Person { pname = n, age = a }
  | a >= 18 = n ++ " (adult)"
  | otherwise = n ++ " (minor)"

total :: Meters -> Meters -> Meters
total (Meters a) (Meters b) = Meters (a + b)

main :: IO ()
main = do
  let p = Person { pname = "Ada", age = 17 }
  putStrLn (describe p)
  putStrLn (describe (older p))
  print (age (older (older p)))
  print (addV (V 1 2) (V 3 4))
  let Meters m = total (Meters 1.5) (Meters 2.25)
  print m
  let z = Complex 1 2 * Complex 3 4 + 1
  print (re z, im z)
  print (sum [1 .. 100 :: Integer], product [1 .. 20 :: Integer])

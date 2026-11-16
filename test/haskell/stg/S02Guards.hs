module Main where

classify :: Int -> String
classify n
  | n < 0 = "negative"
  | n == 0 = "zero"
  | otherwise = "positive"

fact :: Int -> Int
fact 0 = 1
fact n = n * fact (n - 1)

fizz :: Int -> String
fizz n
  | n `mod` 15 == 0 = "FizzBuzz"
  | n `mod` 3 == 0 = "Fizz"
  | n `mod` 5 == 0 = "Buzz"
  | otherwise = show n

vowel :: Char -> Bool
vowel 'a' = True
vowel 'e' = True
vowel 'i' = True
vowel 'o' = True
vowel 'u' = True
vowel _ = False

lookupL :: Int -> [(Int, String)] -> String
lookupL k ps
  | Just v <- lookup k ps = v
  | otherwise = "none"
  where unused = k

main :: IO ()
main = do
  mapM_ (putStrLn . classify) [-3, 0, 8]
  print (fact 10)
  mapM_ (putStrLn . fizz) [1 .. 15]
  print (filter vowel "functional programming")
  putStrLn (lookupL 2 [(1, "one"), (2, "two")])
  putStrLn (lookupL 9 [(1, "one")])

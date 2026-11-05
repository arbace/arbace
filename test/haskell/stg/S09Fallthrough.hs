module Main where

-- guards that fail into the next equation, nested patterns, Integer literal patterns,
-- string patterns, Either, local mutual recursion, pattern bindings
grade :: Int -> Int -> String
grade x y | x > 90, y > 90 = "both"
grade x _ | x > 90 = "first"
grade _ y | y > 90 = "second"
grade _ _ = "neither"

merge :: [Int] -> [Int] -> [Int]
merge [] ys = ys
merge xs [] = xs
merge (x : xs) (y : ys)
  | x <= y = x : merge xs (y : ys)
  | otherwise = y : merge (x : xs) ys

fib :: Integer -> Integer
fib 0 = 0
fib 1 = 1
fib n = fib (n - 1) + fib (n - 2)

command :: String -> String
command "start" = "starting"
command "stop" = "stopping"
command ('g' : 'o' : ' ' : rest) = "going to " ++ rest
command other = "unknown: " ++ other

parse :: String -> Either String Int
parse s = case reads s of
  [(n, "")] -> Right n
  _ -> Left ("bad: " ++ s)

nested :: [(Int, Maybe (Char, Bool))] -> [String]
nested [] = []
nested ((n, Just (c, True)) : rest) = (show n ++ [c]) : nested rest
nested ((n, Just (_, False)) : rest) = show (negate n) : nested rest
nested ((_, Nothing) : rest) = "-" : nested rest

parity :: Int -> (Bool, Bool)
parity n = (ev n, od n)
  where
    ev 0 = True
    ev k = od (k - 1)
    od 0 = False
    od k = ev (k - 1)

stats :: [Double] -> (Double, Double)
stats xs = (mean, sqrt var)
  where
    n = fromIntegral (length xs)
    mean = sum xs / n
    var = sum [(x - mean) ^ (2 :: Int) | x <- xs] / n

main :: IO ()
main = do
  mapM_ (putStrLn . uncurry grade) [(95, 99), (95, 10), (10, 95), (1, 2)]
  print (merge [1, 4, 9, 10] [2, 3, 11])
  print (map fib [0 .. 15])
  mapM_ (putStrLn . command) ["start", "stop", "go home", "jump"]
  print (map parse ["42", "x1", "-7"])
  print (nested [(1, Just ('a', True)), (2, Just ('b', False)), (3, Nothing)])
  print (parity 7, parity 10)
  let (m, sd) = stats [2, 4, 4, 4, 5, 5, 7, 9]
  print m
  print sd
  print (words "a bc  def", unwords ["x", "y"], zip3 [1 :: Int ..] "ab" [True, False])
  print (concatMap (replicate 2) "abc", show (-5 :: Int), [minBound, maxBound :: Int])

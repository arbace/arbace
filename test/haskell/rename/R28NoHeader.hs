import Data.List (sort)

main :: IO ()
main = do
  let xs = sort [3, 1, 2 :: Int]
  print xs
  mapM_ putStrLn ["a", "b"]

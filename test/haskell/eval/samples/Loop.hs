main :: IO ()
main = do
  print (foldl (+) 0 [1..3000000::Integer])
  let x = x + 1 :: Int
  print x

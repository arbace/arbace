module R29Where where

collatz :: Int -> Int
collatz n = go n 0
  where
    go 1 acc = acc
    go m acc
      | even m = go (m `div` 2) (acc + 1)
      | otherwise = go (3 * m + 1) (acc + 1)

area :: Double -> Double -> Double
area w h = w * h * k
  where k = 1

classify :: Int -> String
classify n
  | n < lo = "low"
  | n < hi = "mid"
  | otherwise = "high"
  where
    lo = 10
    hi = lo * 10

outer :: Int -> Int
outer x = inner x + other
  where
    inner y = y + helper y
      where helper z = z * x
    other = 1

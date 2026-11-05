module R03Sections where

a1 :: [Int] -> [Int]
a1 = map (+ 1)

a2 :: [Int] -> [Int]
a2 = map (2 *)

a3 :: [Int] -> [Int]
a3 = map (subtract 1) . map (\x -> x - 1)

a4 :: Int -> Bool
a4 = (== 3) . (`mod` 5) . (+ 1) . negate

a5 :: [Bool] -> Bool
a5 = foldr (&&) True

a6 :: Int -> Int
a6 x = (x `div`) (x + 1)

a7 :: Maybe Int -> Int
a7 m = maybe 0 (+ 1) $ fmap (* 2) m

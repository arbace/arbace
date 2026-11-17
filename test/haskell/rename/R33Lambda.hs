{-# LANGUAGE LambdaCase #-}
module R33Lambda where

compose :: [a -> a] -> a -> a
compose = foldr (.) id

flipC :: (a -> b -> c) -> b -> a -> c
flipC f = \b a -> f a b

pairs :: [a] -> [(a, a)]
pairs xs = zip xs (tail xs)

catter :: [Maybe Int] -> [Int]
catter = concatMap (\case Just x -> [x]; Nothing -> [])

shadow :: Int -> Int
shadow x = (\x -> (\x -> x + 1) x) x

letPoly :: (Int, Bool)
letPoly = let i y = y in (i 1, i True)

ops :: Int
ops = (subtract 1 . (* 2) . (+ 3)) 4

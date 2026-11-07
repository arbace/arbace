module T13Misc where

fact :: Integer -> Integer
fact 0 = 1
fact n = n * fact (n - 1)

fibs = 0 : 1 : zipWith (+) fibs (tail fibs)

apply2 f = f . f

strict x y = x `seq` y

composeAll fs = foldr (.) id fs

dup x = [x, x]

firstJust (Just x : _) = Just x
firstJust (Nothing : xs) = firstJust xs
firstJust [] = Nothing

halve x = x / 2

roundTrip n = fromIntegral (round n :: Int)

eitherLen :: Either String [a] -> Int
eitherLen (Left s) = length s
eitherLen (Right xs) = length xs

triple = (,,) 1 'a' True

charCode c = fromEnum c + 1

powers = [2 ^ i | i <- [0 .. 10]]

zipped = zip [1 ..] "abc"

until10 = until (> 10) (* 2) 1

showAll xs = unwords (map show xs)

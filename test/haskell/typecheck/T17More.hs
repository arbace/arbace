module T17More where

class Container f where
  cmember :: Eq a => a -> f a -> Bool
  csize :: f a -> Int

newtype Bag a = Bag [a]

instance Container Bag where
  cmember x (Bag xs) = x `elem` xs
  csize (Bag xs) = length xs

heads ms = do
  (x : _) <- ms
  return x

readPlus s = read s + 1

ratio :: Int -> Int -> Double
ratio a b = fromIntegral a / fromIntegral b

digitsOf n = if n < 10 then [n] else digitsOf (n `div` 10) ++ [n `mod` 10]

letters = ['a' .. 'e']

conv x = realToFrac x * 2

collect :: IO [String]
collect = mapM return ["a", "b"]

safeHead [] = Nothing
safeHead (x : _) = Just x

firsts xss = [x | (x : _) <- xss]

instance Container Maybe where
  cmember x m = m == Just x
  csize Nothing = 0
  csize (Just _) = 1

data Op = Plus | Minus deriving (Eq, Show)

apply Plus = (+)
apply Minus = (-)

showOp o = show o ++ "!"

opsEq = Plus == Minus

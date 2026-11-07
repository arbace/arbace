module R02Fixity where

infixr 5 +++
infixl 6 `plus`, |>
infix 4 ===
infixr 0 $$

(+++) :: [a] -> [a] -> [a]
xs +++ ys = foldr (:) ys xs

plus :: Int -> Int -> Int
plus a b = a + b

(|>) :: a -> (a -> b) -> b
x |> f = f x

(===) :: Eq a => a -> a -> Bool
a === b = a == b

($$) :: (a -> b) -> a -> b
f $$ x = f x

t1 :: [Int]
t1 = [1] +++ [2] +++ [3, 4]

t2 :: Int
t2 = 1 `plus` 2 `plus` 3 * 4

t3 :: Bool
t3 = 1 + 2 === 3 && True || False

t4 :: Int
t4 = succ $$ 1 `plus` 2 |> negate

t5 :: [Int]
t5 = map (subtract 1) [1 .. 3] ++ map (`plus` 1) [2] ++ map (1 `plus`) [3]

t6 :: Int
t6 = - 1 + 2 - 3 `div` 2 + (- 4) * (- 5)

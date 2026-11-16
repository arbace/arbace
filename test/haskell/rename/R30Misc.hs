module R30Misc where

data Op = Plus | Minus deriving (Eq, Show)

eval :: Op -> Int -> Int -> Int
eval Plus = (+)
eval Minus = (-)

seqs :: [[Int]]
seqs = [[1 ..], [1, 3 ..], [1 .. 10], [10, 8 .. 0]] 

cond :: Bool -> Int
cond b = if b then 1 else 2

str :: String
str = "tab\there" ++ ['a', 'b'] ++ show (3 :: Int)

nested :: Int -> Int -> Int
nested = \x -> \y -> x + y `seq` y
  where _unused = ()

lt :: Int -> Int -> Ordering
lt a b = compare a b

twoLets :: Int
twoLets = let a = 1 in let b = a + 1 in a + b

caseof :: Maybe (Maybe Int) -> Int
caseof m = case m of
  Just (Just n) | n > 0 -> n
  Just _ -> 0
  Nothing -> -1

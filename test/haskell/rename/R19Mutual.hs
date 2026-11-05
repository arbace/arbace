module R19Mutual where

data A = A B | NoA
data B = B A | NoB
type S = [Q]
data Q = Q S

class K a where k :: a -> Int
instance K A where k (A b) = k b; k NoA = 0
instance K B where k (B a) = k a; k NoB = 1

a1, a2 :: Int -> Int
a1 n = if n == 0 then 0 else a2 (n - 1)
a2 n = a1 n + a3 n
a3 :: Int -> Int
a3 = const 1
